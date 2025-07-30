package com.ducnh.highperformance.collections;

@FunctionalInterface
public interface IntObjConsumer<T> {
	void accept(int i, T v);
}
