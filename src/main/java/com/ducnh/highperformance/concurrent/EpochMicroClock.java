package com.ducnh.highperformance.concurrent;

@FunctionalInterface
public interface EpochMicroClock {
	long microTime();
}
