package com.ducnh.highperformance.concurrent;

public class SleepingMillisIdleStrategy implements IdleStrategy{
	
	public static final String ALIAS = "sleep-ms";
	public static final long DEFAULT_SLEEP_PERIOD_MS = 1L;
	
	private final long sleepPeriodMs;
	
	public SleepingMillisIdleStrategy() {
		sleepPeriodMs = DEFAULT_SLEEP_PERIOD_MS;
	}
	
	public SleepingMillisIdleStrategy(final long sleepPeriodMs) {
		this.sleepPeriodMs = sleepPeriodMs;
	}
	
	public void idle(final int workCount) {
		if (workCount > 0) {
			return;
		}
		
		try {
			Thread.sleep(sleepPeriodMs);
		} catch (final InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
	
	public void idle() {
		try {
			Thread.sleep(sleepPeriodMs);
		} catch (final InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
	
	public void reset() {
		
	}
	
	public String alias() {
		return ALIAS;
	}
	
	public String toString() {
		return "SleepingMillisIdleStrategy{"
				+ "alias=" + ALIAS + 
				", sleepPeriodMs=" + sleepPeriodMs + 
				'}';
	}
}
