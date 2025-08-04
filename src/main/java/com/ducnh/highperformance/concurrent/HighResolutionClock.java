package com.ducnh.highperformance.concurrent;

import java.time.Instant;

public class HighResolutionClock {
	
	private HighResolutionClock() {
		
	}
	
	public static long epochMillis() {
		return System.currentTimeMillis();
	}
	
	public static long epochMicros() {
		final Instant now = Instant.now();
		final long seconds = now.getEpochSecond();
		final long nanosFromSecond = now.getNano();
		
		return (seconds * 1_000_000) + (nanosFromSecond / 1_000);
	}
	
	public static long epochNanos() {
		final Instant now = Instant.now();
		final long seconds = now.getEpochSecond();
		final long nanosFromSecond = now.getNano();
		
		return (seconds * 1_000_000_000) + nanosFromSecond;
	}
}
