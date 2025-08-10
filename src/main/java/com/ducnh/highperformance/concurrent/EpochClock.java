package com.ducnh.highperformance.concurrent;

@FunctionalInterface
public interface EpochClock {
	long time();
}
