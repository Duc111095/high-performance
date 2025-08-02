package com.ducnh.highperformance.collections;

@FunctionalInterface
public interface ObjIntPredicate<T> {
	boolean test(T valueOne, int valueTwo);
}
