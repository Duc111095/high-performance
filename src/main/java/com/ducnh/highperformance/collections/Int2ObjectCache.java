package com.ducnh.highperformance.collections;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import static com.ducnh.highperformance.collections.CollectionUtil.validatePositivePowerOfTwo;
import static java.util.Objects.requireNonNull;

public class Int2ObjectCache<V> implements Map<Integer, V> {
	
	private long cachePuts = 0;
	private long cacheHits = 0;
	private long cacheMisses = 0;
	
	private int size;
	private final int capacity;
	private final int setSize;
	private final int setSizeShift;
	private final int mask;
	
	private final int[] keys;
	private final Object[] values;
	private final Consumer<V> evictionConsumer;
	
	private ValueCollection valueCollection;
	private KeySet keySet;
	private EntrySet entrySet;
	
	public Int2ObjectCache(
			final int numSets,
			final int setSize,
			final Consumer<V> evictionConsumer) {
		validatePositivePowerOfTwo(numSets);
		validatePositivePowerOfTwo(setSize);
		requireNonNull(evictionConsumer, "null values are not permitted");
		
		if (((long)numSets) * setSize > (Integer.MAX_VALUE - 8)) {
			throw new IllegalArgumentException(
					"total capacity must be <= max array size: numSets=" + numSets + " setSize=" + setSize);
		}
		
		this.setSize = setSize;
		this.setSizeShift = Integer.numberOfTrailingZeros(setSize);
		capacity = numSets << setSizeShift;
		this.evictionConsumer = evictionConsumer;
	}
	
	public long cacheHits() {
		return cacheHits;
	}
	
	public long cacheMisses() {
		return cacheMisses;
	}
	
	public void resetCounters() {
		cacheHits = 0;
		cacheMisses = 0;
		cachePuts = 0;
	}
	
	public int capacity() {
		return capacity;
	}
	
	public int size() {
		return size;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public boolean containsKey(final Object key) {
		return containsKey((int) key);
	}
	
	public boolean containsKey(final int key) {
		boolean found = false;
		final int setNumber = Hashing.hash(key, mask);
		final int setBeginIndex = setNumber << setSizeShift;
		
		final int[] keys = this.keys;
		final Object[] values = this.values;
		for (int i = setBeginIndex, setEndIndex = setBeginIndex + setSize; i < setEndIndex; i++) {
			if (values[i] == null) {
				break;
			}
			if (key == keys[i]) {
				found = true;
				break;
			}
		}
		
		return found;
	}
	
	public boolean containsValue(final Object value) {
		boolean found = false;
		if (value != null) {
			final Object[] values = this.values;
			for (final Object v : values) {
				if (Objects.equals(v, value)) {
					found = true;
					break;
				}
			}
		}
		
		return found;
	}
	
	public V get(final Object key) {
		return get((int) key);
	}
	
	@SuppressWarnings("unchecked")
	public V get(final int key) {
		final int setNumber = Hashing.hash(key, mask);
		final int setBeginIndex = setNumber << setSizeShift;
		
		final int[] keys = this.keys;
		final Object[] values = this.values;
		for (int i = setBeginIndex, setEndIndex = setBeginIndex +  setSize; i < setEndIndex; i++) {
			final Object value = values[i];
			if (value == null) {
				break;
			}
			
			if (key == keys[i]) {
				cacheHits++;
				return (V)value;
			}
		}
		
		cacheMisses++;
		return null;
	}
	
	public V getOrDefault(final int key, final V defaultValue) {
		final V value = get(key);
		return value == null ? defaultValue : value;
	}
	
	public void forEach(final BiConsumer<? super Integer, ? super V> action) {
		forEachInt(action::accept);
	}
	
	@SuppressWarnings("unchecked")
	public void forEachInt(final IntObjConsumer<? super V> consumer) {
		requireNonNull(consumer);
		final int[] keys = this.keys;
		final Object[] values = this.values;
		final int length = values.length;
		int remaining = size;
		
		for (int index = 0; remaining > 0 && index < length; index ++) {
			final Object value = values[index];
			if (null != value) {
				consumer.accept(keys[index], (V)value);
				--remaining;
			}
		}
	}
	
	public V computeIfAbsent(final Integer key, final Function<? super Integer, ? extends V> mappingFunction) {
		return computeIfAbsent((int) key, mappingFunction::apply);
	}
	
	public V computeIfAbsent(final int key, final IntFunction<? extends V> mappingFunction) {
		requireNonNull(mappingFunction);
		V value = get(key);
		if (null == value) {
			value = mappingFunction.apply(key);
			if (null != value) {
				put(key, value);
			}
		}
		
		return value;
	}
	
	public V computeIfPresent(final Integer key, final BiFunction<? super Integer, ? super V, ? extends V> remappingFunction) {
		return computeIfPresent((int)key, remappingFunction::apply);
	}
}
