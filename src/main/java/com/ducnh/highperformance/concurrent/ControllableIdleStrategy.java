package com.ducnh.highperformance.concurrent;

import java.util.concurrent.locks.LockSupport;

import com.ducnh.highperformance.concurrent.status.StatusIndicatorReader;

public final class ControllableIdleStrategy implements IdleStrategy{
	public static final String ALIAS = "controllable";
	
	public static final int NOT_CONTROLLED = 0;
	public static final int NOOP = 1;
	public static final int BUSY_SPIN = 2;
	public static final int YIELD = 3;
	public static final int PARK = 4;
	private static final long PARK_PERIOD_NANOSECONDS = 1000L;
	
	private final StatusIndicatorReader statusIndicator;
	
	public ControllableIdleStrategy(final StatusIndicatorReader statusIndicator) {
		this.statusIndicator = statusIndicator;
	}
	
	public void idle(final int workCount) {
		if (workCount > 0) {
			return;
		}
		
		idle();
	}
	
	public void idle() {
		final int status = (int)statusIndicator.getVolatile();
		
		switch (status) {
		case NOOP:
			break;
		
		case BUSY_SPIN:
			Thread.onSpinWait();
			break;
		
		case YIELD:
			Thread.yield();
			break;
		
		case PARK:
		default:
			LockSupport.parkNanos(PARK_PERIOD_NANOSECONDS);
			break;
		}
	}
	
	public void reset() {
		
	}
	
	public String alias() {
		return ALIAS;
	}
	
	public String toString() {
		return "ControllableIdleStrategy{" + 
				"alias=" + ALIAS + 
				", statusIndicator=" + statusIndicator +
				"}";
	}
}
