package com.ducnh.highperformance.collections;

@FunctionalInterface
public interface IntObjectToObjectFunction<T, R> {
	R apply(int i, T t);
}
