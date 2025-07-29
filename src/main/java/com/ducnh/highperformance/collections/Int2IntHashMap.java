package com.ducnh.highperformance.collections;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

import static java.util.Objects.requireNonNull;
import static com.ducnh.highperformance.BitUtil.findNextPositivePowerOfTwo;
import static com.ducnh.highperformance.collections.CollectionUtil.validateLoadFactor;

public class Int2IntHashMap implements Map<Integer, Integer>{
	static final int MIN_CAPACITY = 8;
	
	private final float loadFactor;
	private final int missingValue;
	private int resizeThreshold;
	private int size = 0;
	private final boolean shouldAvoidAllocation;
	
	private int[] entries;
	private KeySet keySet;
	private ValueCollection values;
	private EntrySet entrySet;
	
	public Int2IntHashMap(final int missingValue) {
		this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, missingValue);
	}
	
	public Int2IntHashMap(final int initialCapacity, 
			final float loadFactor,
			final int missingValue) {
		this(initialCapcity, loadFactor, missingValue, true);
	}
	
	public Int2IntHashMap(
			final int initialCapacity,
			final float loadFactor,
			final int missingValue,
			final boolean shouldAvoidAllocation) {
		validateLoadFactor(loadFactor);
		
		this.loadFactor = loadFactor;
		this.missingValue = missingValue;
		this.shouldAvoidAllocation = shouldAvoidAllocation;
		
		capacity(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity)));
	}
	
	public Int2IntHashMap(final Int2IntHashMap mapToCopy) {
			this.loadFactor = mapToCopy.loadFactor;
			this.resizeThreshold = mapToCopy.resizeThreshold;
			this.size = mapToCopy.size;
			this.shouldAvoidAllocation = mapToCopy.shouldAvoidAllocation;
			this.missingValue = mapToCopy.missingValue;
			
			entries = mapToCopy.entries.clone();
	}
	
	public int missingValue() {
		return missingValue;
	}
	
	public float loadFactor() {
		return loadFactor;
	}
	
	public int capacity() {
		return entries.length >> 1;
	}
	
	public int resizeThreshold() {
		return resizeThreshold;
	}
	
	public int size() {
		return size;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public int getOrDefault(final int key, final int defaultValue) {
		final int value = get(key);
		return missingValue != value ? value : defaultValue;
	}
	
	public int get(final int key) {
		final int missingValue = this.missingValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int index = Hashing.evenHash(key, mask);
		
		int value;
		while (missingValue != (value = entries[index + 1])) {
			if (key == entries[index]) {
				break;
			}
			index = next(index, mask);
		} 
		
		return value;
	}
	
	public int put(final int key, final int value) {
		final int missingValue = this.missingValue;
		if (missingValue == value) {
			throw new IllegalArgumentException("cannot except missingValue");
		}
		
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int index = Hashing.evenHash(key, mask);
		
		int oldValue;
		while (missingValue != (oldValue = entries[index + 1])) {
			if (key == entries[index]){
				break;
			}
			
			index = next(index, mask);
		}
		
		if (missingValue == oldValue) {
			++size;
			entries[index] = key;
		}
		
		entries[index + 1] = value;
		increaseCapacity();
		return oldValue;
	}
	
	public int putIfAbsent(final int key, final int value) {
		final int missingValue = this.missingValue;
		if (missingValue == value) {
			throw new IllegalArgumentException("cannot accept missingValue");
		}
		
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int index = Hashing.evenHash(key, mask);
		
		int oldValue;
		while (missingValue != (oldValue = entries[index + 1])) {
			if (key == entries[index]) {
				return oldValue;
			}
			
			index = next(index, mask);
		}
		
		++size;
		entries[index] = key;
		entries[index + 1] = value;
		
		increaseCapacity();
		return oldValue;
	}
	
	private void increaseCapacity() {
		if (size > resizeThreshold) {
			final int newCapacity = entries.length;
			rehash(newCapacity);
		}
	}
	
	private void rehash(final int newCapacity) {
		final int missingValue = this.missingValue;
		final int[] oldEntries = entries;
		final int length = oldEntries.length;
		
		capacity(newCapacity);
		
		final int[] newEntries = entries;
		final int mask = newEntries.length - 1;
		
		for (int valueIndex = 1; valueIndex < length; valueIndex += 2) {
			final int value = oldEntries[valueIndex];
			if (missingValue != value) {
				final int key = oldEntries[valueIndex - 1];
				int newKeyIndex = Hashing.evenHash(key, mask);
				
				while (missingValue != newEntries[newKeyIndex + 1]) {
					newKeyIndex = next(newKeyIndex, mask);
				}
				
				newEntries[newKeyIndex] = key;
				newEntries[newKeyIndex + 1] = value;
			}
		}
	}
	
	@Deprecated
	public void intForEach(final IntIntConsumer consumer) {
		forEachInt(consumer);
	}
	
	public void forEachInt(final IntIntConsumer consumer) {
		requireNonNull(consumer);
		final int missingValue = this.missingValue;
		final int[] entries = this.entries;
		final int length = entries.length;
		
		for (int valueIndex = 1, remaining = size; remaining > 0 && valueIndex < length; valueIndex += 2) {
			if (missingValue != entries[valueIndex]) {
				consumer.accept(entries[valueIndex - 1], entries[valueIndex]);
				--remaining;
			}
		}
	}
	
	public boolean containsKey(final int key) {
		return missingValue != get(key);
	}
	
	public boolean containsValue(final int value) {
		boolean found = false;
		final int missingValue = this.missingValue;
		if (missingValue != value) {
			final int[] entries = this.entries;
			final int length = entries.length;
			int remaining = size;
			for (int valueIndex = 1; remaining > 0 && valueIndex < length; valueIndex += 2) {
				final int existingValue = entries[valueIndex];
				if (missingValue != existingValue) {
					if (existingValue == value) {
						found = true;
						break;
					}
					-- remaining;
				}
			}
		}
		return found;
	}
	
	public void compact() {
		final int idealCapacity = (int) Math.round(size() * (1.0d / loadFactor));
		rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
	}
	
	public int computeIfAbsent(final int key, final IntUnaryOperator mappingFunction) {
		requireNonNull(mappingFunction);
		final int missingValue = this.missingValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int index = Hashing.evenHash(key, mask);
		
		int value;
		while (missingValue != (value = entries[index + 1])) {
			if (key == entries[index]) {
				break;
			}
			
			index = next(index, mask);
		}
		
		if (missingValue == value && missingValue != (value = mappingFunction.applyAsInt(key))) {
			entries[index] = key;
			entries[index + 1] = value;
			++size;
			increaseCapacity();
		}
		return value;
	}
	
	public int computeIfPresent(final int key, final IntBinaryOperator remappingFunction) {
		requireNonNull(remappingFunction);
		final int missingValue = this.missingValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int index = Hashing.evenHash(key, mask);
		
		int value;
		while (missingValue != (value = entries[index + 1])) {
			if (key == entries[index]) {
				break;
			}
			index = next(index, mask);
		}
		if (missingValue != value) {
			value = remappingFunction.applyAsInt(key, value);
			entries[index + 1] = value;
			if (missingValue == value) {
				size--;
				compactChain(index);
			}
		}
		return value;
	}
	
	public int compute(final int key, final IntBinaryOperator remappingFunction) {
		requireNonNull(remappingFunction);
		final int missingValue = this.missingValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int index = Hashing.evenHash(key, mask);
		
		int oldValue;
		while (missingValue != (oldValue = entries[index + 1])) {
			if (key == entries[index]) {
				break;
			}
			
			index = next(index, mask);
		}
		
		final int newValue = remappingFunction.applyAsInt(key, oldValue);
		if (missingValue != newValue) {
			entries[index + 1] = newValue;
			if (oldValue == missingValue) {
				entries[index] = key;
				++size;
				increaseCapacity();
			}
		} else if (missingValue != oldValue) {
			entries[index + 1] = missingValue;
			size--;
			compactChain(index);
		}
		return newValue;
	}
	
	public Integer get(final Object key) {
		return valOrNull(get((int)key));
	}
	
	public Integer put(final Integer key, final Integer value) {
		return valOrNull(put((int)key, (int)value));
	}
	
	public void forEach(final BiConsumer<? super Integer, ? super Integer> action) {
		forEachInt(action::accept);
	}
	
	public boolean containsKey(final Object key) {
		return containsKey((int)key);
	}
	
	public boolean containsValue(final Object value) {
		return containsValue((int)value);
	}
	
	public void putAll(final Map<? extends Integer, ? extends Integer> map) {
		for (final Map.Entry<? extends Integer, ? extends Integer> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}
	
	public void putAll(final Int2IntHashMap map) {
		final EntryIterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			it.findNext();
			put(it.getIntKey(), it.getIntValue());
		}
	}
	
	public Integer putIfAbsent(final Integer key, final Integer value) {
		return valOrNull(putIfAbsent((int) key, (int) value));
	}
	
} 
