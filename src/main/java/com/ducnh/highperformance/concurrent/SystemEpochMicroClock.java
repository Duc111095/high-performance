package com.ducnh.highperformance.concurrent;

public class SystemEpochMicroClock implements EpochMicroClock{
	public SystemEpochMicroClock() {
		
	}
	
	public long microTime() {
		return HighResolutionClock.epochMicros();
	}
}
