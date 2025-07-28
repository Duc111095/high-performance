package com.ducnh.highperformance.collections;

import static com.ducnh.highperformance.BitUtil.findNextPositivePowerOfTwo;
import static com.ducnh.highperformance.collections.CollectionUtil.validateLoadFactor;

public class Int2IntCounterMap {
	private static final int MIN_CAPACITY = 8;
	private final float loadFactor;
	private final int initialValue;
	private int resizeThreshold;
	private int size = 0;
	
	private int[] entries;
	
	public Int2IntCounterMap(final int initialValue) {
		this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, initialValue);
	}
	
	public Int2IntCounterMap(
			final int initialCapacity,
			final float loadFactor, 
			final int initialValue) {
		validateLoadFactor(loadFactor);
		
		this.loadFactor = loadFactor;
		this.initialValue = initialValue;
		
		capacity(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity)));
	}
	
	public int initialValue() {
		return initialValue;
	}
	
	public float loadFactor() {
		return loadFactor;
	}
	
	public int resizeThreshold() {
		return resizeThreshold;
	}
	
	public int capacity () {
		return entries.length >> 1;
	}
	
	public int size() {
		return size;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public int get(final int key) {
		final int initialValue = this.initialValue;
		final int[] entries = this.entries;
		final int mask = entries.length;
		int index = Hashing.evenHash(key, mask);
		int value;
		while (initialValue != (value = entries[index + 1])) {
			if (key == entries[index]) {
				break;
			}
			index = next(index, mask);
		}
		return value;
	}
	
	public int put(final int key, final int value) {
		final int initialValue = this.initialValue;
		if (initialValue == value) {
			throw new IllegalArgumentException("cannot accept initialValue");
		}
		
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int index = Hashing.evenHash(key, mask);
		
		int oldValue;
		while (initialValue != (oldValue = entries[index + 1])) {
			if (key == entries[index]) {
				break;
			}
			index = next(index, mask);
		}
		
		if (initialValue == oldValue) {
			++size;
			entries[index] = key;
		}
		
		entries[index + 1] = value;
		increaseCapacity();
		return oldValue;
	}
	
	public int incrementAndGet(final int key) {
		return addAndGet(key, 1);
	}
	
	public int decrementAndGet(final int key) {
		return andAndGet(key, -1);
	}
	
	public int addAndGet(final int key, final int amount) {
		return getAndAdd(key, amount) + amount;
	}
	
	public int getAndIncrement(final int key) {
		return getAndAdd(key, 1);
	}
	
	final int getAndDecrement(final int key) {
		return getAndAdd(key, -1);
	}
	
	public int getAndAdd(final int key, final int amount) {
		final int initialValue = this.initialValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int index = Hashing.evenHash(key, mask);
		
		int oldValue;
		while (initialValue != (oldValue = entries[index + 1])) {
			if (key == entries[index]) {
				break;
			}
			
			index = next(index, mask);
		}
		
		if (amount != 0) {
			final int newValue = oldValue + amount;
			entries[index + 1] = newValue;
			
			if (initialValue == oldValue) {
				++size;
				entries[index] = key;
				increaseCapacity();
			} else if (initialValue == newValue) {
				size--;
				compactChain(index);
			}
		}
		return oldValue;
	}
	
	public void forEach(final IntIntConsumer consumer) {
		final int initialValue = this.initialValue;
	}
}
