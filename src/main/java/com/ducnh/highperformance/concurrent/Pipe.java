package com.ducnh.highperformance.concurrent;

import java.util.Collection;
import java.util.function.Consumer;

public interface Pipe<E> {
	long addedCount();
	long removedCount();
	int capacity();
	int size();
	int remainingCapacity();
	int drain(Consumer<E> elementConsumer);
	int drain(Consumer<E> elementConsumer, int limit);
	int drainTo(Collection<? super E> target, int limit);
}
