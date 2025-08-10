package com.ducnh.highperformance.concurrent;

public class SystemEpochNanoClock implements EpochNanoClock{
	public static final SystemEpochNanoClock INSTANCE = new SystemEpochNanoClock();
	
	public SystemEpochNanoClock() {
		
	}
	
	public long nanoTime() {
		return HighResolutionClock.epochNanos();
	}
}
