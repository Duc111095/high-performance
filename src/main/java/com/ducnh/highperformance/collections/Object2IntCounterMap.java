package com.ducnh.highperformance.collections;

import static com.ducnh.highperformance.BitUtil.findNextPositivePowerOfTwo;
import static com.ducnh.highperformance.collections.CollectionUtil.validateLoadFactor;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.ToIntFunction;

public class Object2IntCounterMap<K> {
	
	private static final int MIN_CAPACITY = 8;
	
	private final float loadFactor;
	private final int initialValue;
	private int resizeThreshold;
	private int size = 0;
	
	private K[] keys;
	private int[] values;
	
	public Object2IntCounterMap(final int initialValue) {
		this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, initialValue);
	}
	
	@SuppressWarnings("unchecked")
	public Object2IntCounterMap(
			final int initialCapacity,
			final float loadFactor, 
			final int initialValue) {
		validateLoadFactor(loadFactor);
		
		this.loadFactor = loadFactor;
		this.initialValue = initialValue;
		
		final int capacity = findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity));
		
		keys = (K[]) new Object[capacity];
		values = new int[capacity];
		Arrays.fill(values, initialValue);
		resizeThreshold = (int)(capacity * loadFactor);
	}
	
	public int initialValue() {
		return initialValue;
	}
	
	public int resizeThreshold() {
		return values.length;
	}
	
	public int size() {
		return size;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public int get(final K key) {
		final int intiialValue = this.initialValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int value;
		while (initialValue != (value = values[index])) {
			if (Objects.equals(keys[index], key)) {
				break;
			}
			
			index = ++index & mask;
		}
		
		return value;
	}
	
	public int put(final K key, final int value) {
		final int initialValue = this.initialValue;
		if (initialValue == value) {
			throw new IllegalArgumentException("cannot accept initialValue");
		}
		
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		int oldValue = initialValue;
		
		while (values[index] != initialValue) {
			if (Objects.equals(keys[index], key)) {
				oldValue = values[index];
				break;
			}
			
			index = ++index & mask;
		}
		
		if (oldValue == initialValue) {
			++size;
			keys[index] = key;
		}
		
		values[index] = value;
		
		increaseCapacity();
		
		return oldValue;
	}
	
	public int incrementAndGet(final K key) {
		return addAndGet(key, 1);
	}
	
	public int decrementAndGet(final K key) {
		return addAndGet(key, -1);
	}
	
	public int addAndGet(final K key, final int amount) {
		return getAndAdd(key, amount) + amount;
	}
	
	public int getAndIncrement(final K key) {
		return getAndAdd(key, 1);
	}
	
	public int getAndDecrement(final K key) {
		return getAndAdd(key, -1);
	}
	
	public int getAndAdd(final K key, final int amount) {
		final int initialValue = this.initialValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		int oldValue = initialValue;
		
		while (initialValue != values[index]) {
			if (Objects.equals(keys[index], key)) {
				oldValue = values[index];
				break;
			}
			
			index = ++index & mask;
		}
		
		if (amount != 0) {
			final int newValue = oldValue + amount;
			values[index] = newValue;
			
			if (initialValue == oldValue) {
				++size;
				keys[index] = key;
				increaseCapacity();
			} else if (initialValue == newValue) {
				size --;
				compactChain(index);
			}
		}
		
		return oldValue;
	}
	
	public void forEach(final ObjIntConsumer<K> consumer) {
		final int initialValue = this.initialValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int length = values.length;
		int remaining = size;
		
		for (int i = 0; remaining > 0 && i < length; i++) {
			if (initialValue != values[i]) {
				consumer.accept(keys[i], values[i]);
				--remaining;
			}
		}
	}
	
	public boolean containsKey(final K key) {
		return initialValue != get(key);
	}
	
	public boolean containsValue(final int value) {
		boolean found = false;
		if (initialValue != value) {
			for (final int v : values) {
				if (value == v) {
					found = true;
					break;
				}
			}
		}
		return found;
	}
	
	public void compact() {
		final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
		rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
	}
	
	public int computeIfAbsent(final K key, final ToIntFunction<? super K> mappingFunction) {
		int value = get(key);
		
		if (value == initialValue) {
			value = mappingFunction.applyAsInt(key);
			if (initialValue != value) {
				put(key, value);
			}
		}
		
		return value;
	}
	
	public int remove(final K key) {
		final int initialValue = this.initialValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int oldValue = initialValue;
		while (initialValue != values[index]) {
			if (Objects.equals(keys[index], key)) {
				oldValue = values[index];
				values[index] = initialValue;
				size--;
				
				compactChain(index);
				
				break;
			}
			
			index = ++index & mask;
		}
		
		return oldValue;
	}
	
	public int minValue() {
		final int initialValue = this.initialValue;
		int min = 0 == size ? initialValue : Integer.MAX_VALUE;
		
		for (final int value : values) {
			if (initialValue != value) {
				min = Math.min(min, value);
			}
		}
		
		return min;
	}
	
	public int maxValue() {
		final int initialValue = this.initialValue;
		int max = 0 == size ? initialValue : Integer.MIN_VALUE;
		
		for (final int value : values) {
			if (initialValue != value) {
				max = Math.max(value, max);
			}
		}
		
		return max;
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append('{');
		
		final int initialValue = this.initialValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int length = values.length;
		
		for (int i = 0; i < length; i++) {
			final int value = values[i];
			if (initialValue != value) {
				sb.append(keys[i]).append('=').append(value).append(", ");
			}
		}
		
		if (sb.length() > 2) {
			sb.setLength(sb.length() - 2);
		}
		
		sb.append('}');
		
		return sb.toString();
	}
	
	@SuppressWarnings("FinalParameters")
	private void compactChain(int deleteIndex) {
		final int initialValue = this.initialValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = deleteIndex;
		
		while(true) {
			index = ++index & mask;
			final int value = values[index];
			if (initialValue == value) {
				break;
			}
			
			final K key = keys[index];
			final int hash = Hashing.hash(key, mask);
			
			if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) || 
					(hash <= deleteIndex && deleteIndex <= index)) {
				keys[deleteIndex] = key;
				values[deleteIndex] = value;
				
				keys[index] = null;
				values[index] = initialValue;
				deleteIndex = index;
			}
		}
	}
	
	private void increaseCapacity() {
		if (size > resizeThreshold) {
			final int newCapacity = values.length * 2;
			rehash(newCapacity);
		}
	}
	
	private void rehash(final int newCapacity) {
		final int mask = newCapacity - 1;
		resizeThreshold = (int)(newCapacity * loadFactor);
		
		@SuppressWarnings("unchecked")
		final K[] tempKeys = (K[]) new Object[newCapacity];
		final int[] tempValues = new int[newCapacity];
		final int initialValue = this.initialValue;
		Arrays.fill(tempValues, initialValue);
		
		final K[] keys = this.keys;
		final int[] values = this.values;
		
		for (int i = 0, size = values.length; i < size; i++) {
			final int value = values[i];
			if (initialValue != value) {
				final K key = keys[i];
				int index = Hashing.hash(key, mask);
				while (initialValue != tempValues[index]) {
					index = ++index & mask;
				}
				
				tempKeys[index] = key;
				tempValues[index] = value;
			}
		}
		
		this.keys = tempKeys;
		this.values = tempValues;
	}
}
