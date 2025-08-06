package com.ducnh.highperformance.concurrent;

@FunctionalInterface
public interface NanoClock {
	long nanoTime();
}
