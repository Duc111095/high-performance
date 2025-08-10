package com.ducnh.highperformance.concurrent;

@FunctionalInterface
public interface EpochNanoClock {
	long nanoTime();
}
