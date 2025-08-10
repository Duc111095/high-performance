package com.ducnh.highperformance.concurrent;

public class SystemEpochClock implements EpochClock{
	
	public static final SystemEpochClock INSTANCE = new SystemEpochClock();
	
	public SystemEpochClock() {
		
	}
	
	public long time() {
		return System.currentTimeMillis();
	}
}
