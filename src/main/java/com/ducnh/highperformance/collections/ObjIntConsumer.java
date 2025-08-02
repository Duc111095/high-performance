package com.ducnh.highperformance.collections;

@FunctionalInterface
public interface ObjIntConsumer<T> {
	void accept(T i, int v);
}
