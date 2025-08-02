package com.ducnh.highperformance.collections;

import java.util.function.Consumer;
import java.util.function.IntFunction;

public final class IntLruCache<E> implements AutoCloseable{
	private final int capacity;
	private final IntFunction<E> factory;
	private final Consumer<E> closer;
	private final int[] keys;
	private final Object[] values;
	
	private int size;
	
	public IntLruCache(
			final int capacity,
			final IntFunction<E> factory, 
			final Consumer<E> closer) {
		this.capacity = capacity;
		this.factory = factory;
		this.closer = closer;
		keys = new int[capacity];
		values = new Object[capacity];
		
		size = 0;
	}
	
	@SuppressWarnings("unchecked")
	public E lookup(final int key) {
		int size = this.size;
		final int[] keys = this.keys;
		final Object[] values = this.values;
		
		for (int i = 0; i < size; i++) {
			if (key == keys[i]) {
				final E value = (E) values[i];
				
				makeMostRecent(key, value, i);
				return value;
			}
		}
		
		final E value = factory.apply(key);
		
		if (value != null) {
			if (capacity == size) {
				closer.accept((E)values[size - 1]);
			} else {
				size ++;
				this.size = size;
			}
			
			makeMostRecent(key, value, size - 1);
		}
		
		return value;
	}
	
	private void makeMostRecent(
			final int key,
			final Object value,
			final int fromIndex) {
		final int[] keys = this.keys;
		final Object[] values = this.values;
		
		for (int i = fromIndex; i > 0; i--) {
			keys[i] = keys[i - 1];
			values[i] = values[i - 1];
		}
		
		keys[0] = key;
		values[0] = value;
	}
	
	public int capacity() {
		return capacity;
	}
	
	@SuppressWarnings("unchecked")
	public void close() {
		final Consumer<E> closer = this.closer;
		final Object[] values = this.values;
		for(int i = 0, size = this.size; i < size; i++) {
			closer.accept((E)values[i]);
		}
	}
}
