package com.ducnh.highperformance.collections;

@FunctionalInterface
public interface ObjectIntToIntFunction<T> {
	int apply(T t, int i);
}
