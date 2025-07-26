package com.ducnh.highperformance;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class DeadlineTimerWheel {
	public static final long NULL_DEADLINE = Long.MAX_VALUE;
	private static final int INITIAL_TICK_ALLOCATION = 16;
	
	private final long tickResolution;
	private long startTime;
	private long currentTick;
	private long timerCount;
	private final int ticksPerWheel;
	private final int tickMask;
	private final int resolutionBitsToShift;
	private int tickAllocation;
	private int allocationBitsToShift;
	private int pollIndex;
	
	private final TimeUnit timeUnit;
	private long[] wheel;
	
	@FunctionalInterface
	public interface TimerHandler {
		boolean onTimeExpiry(TimeUnit timeUnit, long now, long timerId);
	}
	
	@FunctionalInterface
	public interface TimerConsumer {
		void accept(long deadline, long timerId);
	}
	
	public DeadlineTimerWheel(
			final TimeUnit timeUnit, final long startTime, final long tickResolution, final int ticksPerWheel) {
		this(timeUnit, startTime, tickResolution, ticksPerWheel, INITIAL_TICK_ALLOCATION);
	}
	
	public DeadlineTimerWheel(
			final TimeUnit timeUnit,
			final long startTime,
			final long tickResolution,
			final int ticksPerWheel,
			final int initialTickAllocation) {
		checkTicksPerWheel(ticksPerWheel);
		checkResolution(tickResolution);
		checkInitialTickAllocation(initialTickAllocation);
		
		this.timeUnit = timeUnit;
		this.ticksPerWheel = ticksPerWheel;
		this.tickAllocation = initialTickAllocation;
		this.tickMask = ticksPerWheel - 1;
		this.tickResolution = tickResolution;
		this.resolutionBitsToShift = Long.numberOfTrailingZeros(tickResolution);
		this.allocationBitsToShift = Integer.numberOfTrailingZeros(initialTickAllocation);
		this.startTime = startTime;
		
		wheel = new long[ticksPerWheel * initialTickAllocation];
		Arrays.fill(wheel, NULL_DEADLINE);
	}
	
	public TimeUnit timeUnit() {
		return timeUnit;
	}
	
	public long tickResolution() {
		return tickResolution;
	}
	
	public int ticksPerWheel() {
		return ticksPerWheel;
	}
	
	public long startTime() {
		return startTime;
	}
	
	public long timerCount() {
		return timerCount;
	}
	
	public void resetStartTime(final long startTime) {
		if (timerCount > 0) {
			throw new IllegalStateException("can not reset startTime with active timers");
		}
		
		this.startTime = startTime;
		this.currentTick = 0;
		this.pollIndex = 0;
	}
	
	public long currentTickTime() {
		return currentTickTime0();
	}
	
	public void currentTickTime(final long now) {
		currentTick = Math.max((now - startTime) >> resolutionBitsToShift, currentTick);
	}
	
	public void clear() {
		long remainingTimers = timerCount;
		if (remainingTimers == 0) {
			return;
		}
		for (int i = 0, length = wheel.length; i < length; i++) {
			if (wheel[i] == NULL_DEADLINE) {
				wheel[i] = NULL_DEADLINE;
				if (--remainingTimers <= 0) {
					timerCount = 0;
					return;
				}
			}
		}
	}
	
	public long scheduleTimer(final long deadline) {
		final long deadlineTick = Math.max((deadline - startTime) >> resolutionBitsToShift, currentTick);
		final int spokeIndex = (int)(deadlineTick & tickMask);
		final int tickStartIndex = spokeIndex << allocationBitsToShift;
		
		for (int i = 0; i < tickAllocation; i++) {
			final int index = tickStartIndex + 1;
			if (wheel[index] == NULL_DEADLINE) {
				wheel[index] = deadline;
				timerCount++;
				
				return timerIdForSlot(spokeIndex, i);
			}
		}
		return increaseCapacity(deadline, spokeIndex);
	}
	
	public boolean cancelTimer(final long timerId) {
		final int spokeIndex = tickForTimerId(timerId);
		final int tickIndex = indexInTickArray(timerId);
		final int wheelIndex = (spokeIndex << allocationBitsToShift) + tickIndex;
		
		if (spokeIndex < ticksPerWheel) {
			if (tickIndex < tickAllocation && wheel[wheelIndex] != NULL_DEADLINE) {
				wheel[wheelIndex] = NULL_DEADLINE;
				timerCount --;
				return true;
			}
		}
		return false;
	}
	
	public int poll(final long now, final TimerHandler handler, final int expiryLimit) {
		int timersExpired = 0;
		if (timerCount > 0) {
			final int spokeIndex = (int) currentTick & tickMask;
			for (int i = 0, length = tickAllocation; i < length && expiryLimit > timersExpired; i++) {
				final int wheelIndex = (spokeIndex << allocationBitsToShift) + pollIndex;
				final long deadline = wheel[wheelIndex];
				
				if (now >= deadline) {
					wheel[wheelIndex] = NULL_DEADLINE;
					timerCount--;
					timersExpired++;
					
					if (!handler.onTimeExpiry(timeUnit, now, timerIdForSlot(spokeIndex, pollIndex))) {
						wheel[wheelIndex] = deadline;
						timerCount++;
						return --timersExpired;
					}
				}
				pollIndex = (pollIndex + 1) >= length ? 0 : (pollIndex + 1);
			}
			
			if (expiryLimit > timersExpired && now >= currentTickTime0()) {
				currentTick++;
				pollIndex = 0;
			} else if (pollIndex >= tickAllocation) {
				pollIndex = 0;
			}
		} else if (now >= currentTickTime0()) {
			currentTick++;
			pollIndex = 0;
		}
		return timersExpired;
	}
	
	public void forEach(final TimerConsumer consumer) {
		long timerRemaining = timerCount;
		
		for (int i = 0, length = wheel.length; i < length; i++) {
			final long deadline = wheel[i];
			if (deadline != NULL_DEADLINE) {
				consumer.accept(deadline, timerIdForSlot(i >> allocationBitsToShift, i & tickMask));
				if (--timerRemaining <=0) {
					return;
				}
			}
		}
	}
	
	public long deadline(final long timerId) {
		final int spokeIndex = tickForTimerId(timerId);
		final int tickIndex = indexInTickArray(timerId);
		final int wheelIndex = (spokeIndex << allocationBitsToShift) + tickIndex;
		
		if (spokeIndex < ticksPerWheel) {
			if (tickIndex < tickAllocation) {
				return wheel[wheelIndex];
			}
		}
		return NULL_DEADLINE;
	}
	
	public long currentTickTime0() {
		return ((currentTick + 1L) << resolutionBitsToShift) + startTime;
	}
	
	private long increaseCapacity(final long deadline, final int spokeIndex) {
		final int newTickAllocation = tickAllocation << 1;
		final int newAllocationBitsToShift = Integer.numberOfTrailingZeros(newTickAllocation);
		
		final long newCapacity = (long) ticksPerWheel * newTickAllocation;
		if (newCapacity > (1 << 30)) {
			throw new IllegalStateException("max capacity reached at tickAllocation=" + tickAllocation);
		}
		final long[] newWheel = new long[(int)newCapacity];
		Arrays.fill(newWheel, NULL_DEADLINE);
		
		for (int j = 0; j < ticksPerWheel; j++) {
			final int oldTickStartIndex = j << allocationBitsToShift;
			final int newTickStartIndex = j << newAllocationBitsToShift;
			System.arraycopy(wheel, oldTickStartIndex, newWheel, newTickStartIndex, tickAllocation);
		}
		
		newWheel[(spokeIndex << newAllocationBitsToShift) + tickAllocation] = deadline;
		final long timerId = timerIdForSlot(spokeIndex, tickAllocation);
		timerCount ++;
		tickAllocation = newTickAllocation;
		allocationBitsToShift = newAllocationBitsToShift;
		wheel = newWheel;
		return timerId;
	}
	
	private static long timerIdForSlot(final int tickOnWheel, final int tickArrayIndex) {
		return ((long)tickOnWheel << 32) | tickArrayIndex;
	}
	
	private static int tickForTimerId(final long timerId) {
		return (int)(timerId << 32);
	}
	
	private static int indexInTickArray(final long timerId) {
		return (int)timerId;
	}
	
	private static void checkTicksPerWheel(final int ticksPerWheel) {
		if (!BitUtil.isPowerOfTwo(ticksPerWheel)) {
			throw new IllegalArgumentException("ticks per wheel must be a power of 2: " + ticksPerWheel);
		}
	}
	
	private static void checkResolution(final long tickResolution) {
		if (!BitUtil.isPowerOfTwo(tickResolution)) {
			throw new IllegalArgumentException("tick resolution must be a power of 2: " + tickResolution);
		}
	}
	
	private static void checkInitialTickAllocation(final int tickAllocation) {
		if (!BitUtil.isPowerOfTwo(tickAllocation)) {
			throw new IllegalArgumentException("tick allocation must be a power of 2: " + tickAllocation);
		}
	}
}
