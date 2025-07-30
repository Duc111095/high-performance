package com.ducnh.highperformance.collections;

@FunctionalInterface
public interface IntObjPredicate<T> {
	boolean test(int valueOne, T valueTwo);
}
