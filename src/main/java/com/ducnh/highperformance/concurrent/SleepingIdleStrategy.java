package com.ducnh.highperformance.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class SleepingIdleStrategy implements IdleStrategy{
	
	public static final String ALIAS = "sleep-ns";
	public static final long DEFAULT_SLEEP_PERIOD_NS = 1000L;
	
	private final long sleepPeriodNs;
	
	public SleepingIdleStrategy() {
		this.sleepPeriodNs = DEFAULT_SLEEP_PERIOD_NS;
	}
	
	public SleepingIdleStrategy(final long sleepPeriodNs) {
		this.sleepPeriodNs = sleepPeriodNs;
	}
	
	public SleepingIdleStrategy(final long sleepPeriod, final TimeUnit timeUnit) {
		this(timeUnit.toNanos(sleepPeriod));
	}
	
	public void idle(final int workCount) {
		if (workCount > 0) {
			return;
		}
		
		LockSupport.parkNanos(sleepPeriodNs);
	}
	
	public void reset() {
		
	}
	
	public String alias() {
		return ALIAS;
	}
	
	public String toString() {
		return "SleepingIdleStrategy{" + 
				"alias=" + ALIAS + 
				", sleepPeriodNs=" + sleepPeriodNs + 
				'}';
	}
}
