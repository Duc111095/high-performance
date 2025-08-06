package com.ducnh.highperformance.concurrent;

@FunctionalInterface
public interface IdGenerator {
	long nextId();
}
