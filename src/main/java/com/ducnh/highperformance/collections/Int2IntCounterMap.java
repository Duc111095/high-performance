package com.ducnh.highperformance.collections;

import static com.ducnh.highperformance.BitUtil.findNextPositivePowerOfTwo;
import static com.ducnh.highperformance.collections.CollectionUtil.validateLoadFactor;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

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
		return addAndGet(key, -1);
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
		final int[] entries = this.entries;
		final int length = entries.length;
		int remaining = size;
		
		for (int i = 1; remaining > 0 && i < length; i += 2) {
			final int value = entries[i];
			if (initialValue != value) {
				consumer.accept(entries[i-1], value);
				--remaining;
			}
		}
	}
	
	public boolean containsKey(final int key) {
		return initialValue != get(key);
	}
	
	public boolean containsValue(final int value) {
		boolean found = false;
		if (initialValue != value) {
			final int[] entries = this.entries;
			final int length = entries.length;
			for (int i = 1; i < length; i += 2) {
				if (value == entries[i]) {
					found = true;
					break;
				}
			}
		}
		return found;
	}
	
	public void clear() {
		if (size > 0) {
			Arrays.fill(entries, initialValue);
			size = 0;
		}
	}
	
	
	public void compact() {
		final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
		rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
	}
	
	public int computeIfAbsent(final int key, final IntUnaryOperator mappingFunction) {
		int value = get(key);
		if (initialValue == value) {
			value = mappingFunction.applyAsInt(key);
			if (initialValue != value) {
				put(key, value);
			}
		}
		return value;
	}
	
	public int remove(final int key) {
		final int initialValue = this.initialValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = Hashing.evenHash(key, mask);
		
		int oldValue;
		while (initialValue != (oldValue = entries[keyIndex + 1])) {
			if (key == entries[keyIndex]) {
				entries[keyIndex + 1] = initialValue;
				size--;
				
				compactChain(keyIndex);
				break;
			}
			
			keyIndex = next(keyIndex, mask);
		}
		return oldValue;
 	}
	
	public int minValue() {
		final int initialValue = this.initialValue;
		int min = 0 == size ? initialValue : Integer.MAX_VALUE;
		
		final int[] entries = this.entries;
		final int length = entries.length;
		for (int i = 1; i < length; i+=2) {
			final int value = entries[i];
			if (initialValue != value) {
				min = Math.min(min, value);
			}
		}
		return min;
	}
	
	public int maxValue() {
		final int initialValue = this.initialValue;
		int max = 0 == size ? initialValue : Integer.MIN_VALUE;
		
		final int[] entries = this.entries;
		int length = entries.length;
		for (int i = 1; i < length; i += 2 ) {
			final int value = entries[i];
			if (initialValue != value) {
				max = Math.max(max, value);
			}
		}
		return max;
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append('{');
		
		final int initialValue = this.initialValue;
		final int[] entries = this.entries;
		final int length = entries.length;
		
		for (int i = 1; i < length; i += 2) {
			final int value = entries[i];
			if (value != initialValue) {
				sb.append(entries[i - 1]).append('=').append(value).append(", ");
			}
		}
		
		if (sb.length() > 1) {
			sb.setLength(sb.length() - 2);
		}
		sb.append('}');
		
		return sb.toString();
	}
	
	private static int next(final int index, final int mask) {
		return (index + 2) & mask;
	}
	
	@SuppressWarnings("FinalParameters")
	private void compactChain(int deleteKeyIndex) {
		final int initialValue = this.initialValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int index = deleteKeyIndex;
		
		while (true) {
			index = next(index, mask);
			final int value = entries[index + 1];
			if (initialValue == value) {
				break;
			}
			final int key = entries[index];
			final int hash = Hashing.evenHash(key, mask);
			
			if ((index < hash && (hash <= deleteKeyIndex || deleteKeyIndex <= index)) || (
					hash <= deleteKeyIndex && deleteKeyIndex <= index)) {
				entries[deleteKeyIndex] = key;
				entries[deleteKeyIndex + 1] = value;
				entries[index + 1] = initialValue;
				deleteKeyIndex = index;
			}
		}
	}
	
	private void capacity(final int newCapacity) {
		final int entriesLength =  newCapacity * 2;
		if (entriesLength < 0) {
			throw new IllegalStateException("max capacity reacehd at size=" + size);
		}
		
		resizeThreshold = (int) (newCapacity * loadFactor);
		entries = new int[entriesLength];
		Arrays.fill(entries, initialValue);
	}
	
	private void increaseCapacity() {
		if (size > resizeThreshold) {
			final int newCapacity = entries.length;
			rehash(newCapacity);
		}
	}
	
	private void rehash(final int newCapacity) {
		final int initialValue = this.initialValue;
		final int[] oldEntries = entries;
		final int length = oldEntries.length;
		
		capacity(newCapacity);
		final int[] newEntries = entries;
		final int mask = newEntries.length - 1;
		
		for (int valueIndex = 1; valueIndex < length; valueIndex += 2) {
			final int value = oldEntries[valueIndex];
			if (value != initialValue) {
				final int key = oldEntries[valueIndex - 1];
				int newKeyIndex = Hashing.evenHash(key, mask);
				
				while (initialValue != newEntries[newKeyIndex + 1]) {
					newKeyIndex = next(newKeyIndex, mask);
				}
				
				newEntries[newKeyIndex] = key;
				newEntries[newKeyIndex + 1] = value;
			}
		}
	}
}
