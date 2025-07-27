package com.ducnh.highperformance.collections;

import static com.ducnh.highperformance.BitUtil.findNextPositivePowerOfTwo;
import static com.ducnh.highperformance.collections.Hashing.compoundKey;
import static com.ducnh.highperformance.collections.CollectionUtil.validateLoadFactor;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class BiInt2ObjectMap<V> {
	public interface EntryConsumer<V> {
		void accept(int keyPartA, int keyPartB, V value);
	}
	
	public interface EntryFunction<V> {
		V apply(int keyPartA, int keyPartB);
	}
	
	public interface EntryRemap<V, V1> {
		V1 apply(int keyPartA, int keyPartB, V oldValue);
	}
	
	private static final int MIN_CAPACITY = 8;
	
	private final float loadFactor;
	private int resizeThreshold;
	private int size;
	
	private long[] keys;
	private Object[] values;
	
	public BiInt2ObjectMap() {
		this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR);
	}
	
	public BiInt2ObjectMap(final int initialCapacity, final float loadFactor) {
		validateLoadFactor(loadFactor);
		
		this.loadFactor = loadFactor;
		final int capacity = findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity));
		resizeThreshold = (int)(capacity * loadFactor);
		
		keys = new long[capacity];
		values = new Object[capacity];
	}
	
	public int capacity() {
		return values.length;
	}
	
	public float loadFactor() {
		return loadFactor;
	}
	
	public int resizeThreshold() {
		return resizeThreshold;
	}
	
	public void clear() {
		if (size > 0) {
			Arrays.fill(values, null);
			size = 0;
		}
	}
	
	public void compact() {
		final int idealCapacity = (int) Math.round(size() * (1.0 / loadFactor));
		rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
	}
	
	@SuppressWarnings("unchecked")
	public V put(final int keyPartA, final int keyPartB, final V value) {
		final V val = (V)mapNullValue(value);
		final long key = compoundKey(keyPartA, keyPartB);
		requireNonNull(val, "value cannot be null");
		
		final long[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object oldValue;
		while ((oldValue = values[index]) != null) {
			if (keys[index] == key) {
				break;
			}
			index = ++index & mask;
		}
		
		if (oldValue == null) {
			++size;
			keys[index] = key;
		}
		
		values[index] = value;
		
		if (size > resizeThreshold) {
			increaseCapacity();
		}
		
		return unmapNullValue(oldValue);
	}
	
	protected Object mapNullValue(final Object value) {
		return value;
	}
	
	@SuppressWarnings("unchecked")
	protected V unmapNullValue(final Object value) {
		return (V) value;
	}
	
	@SuppressWarnings("unchecked")
	private V getMapping(final int keyPartA, final int keyPartB) {
		final long key = compoundKey(keyPartA, keyPartB);
		final long[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object value;
		while (null != (value = values[index])) {
			if (key == keys[index]) {
				break;
			}
			
			index = ++index & mask;
		}
		
		return (V) value;
	}
	
	public V get(final int keyPartA, final int keyPartB) {
		return unmapNullValue(getMapping(keyPartA, keyPartB));
	}
	
	public V getOrDefault(final int keyPartA, final int keyPartB, final V defaultValue) {
		final V val = getMapping(keyPartA, keyPartB);
		return unmapNullValue(null != val ? val : defaultValue);
	}
	
	public boolean containsKey(final int keyPartA, final int keyPartB) {
		final long key = compoundKey(keyPartA, keyPartB);
		final long[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		boolean found = false;
		while (null != values[index]) {
			if (key == keys[index]) {
				found = true;
				break;
			}
			index = ++index & mask;
		}
		
		return found;
	}
	
	@SuppressWarnings("unchecked")
	public V remove(final int keyPartA, final int keyPartB) {
		final long key = compoundKey(keyPartA, keyPartB);
		final long[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object value;
		while ((value = values[index]) != null) {
			if (key == keys[index]) {
				values[index] = null;
				--size;
				compactChain(index);
				break;
			}
			
			index = ++index & mask;
		}
		
		return (V)value;
	}
	
	public V computeIfAbsent(final int keyPartA, final int keyPartB, final EntryFunction<? extends V> mappingFunction) {
		final long key = compoundKey(keyPartA, keyPartB);
		requireNonNull(mappingFunction);
		final long[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object mappedValue;
		while (null != (mappedValue = values[index])) {
			if (keys[index] == key) {
				break;
			}
			index = ++index & mask;
		}
		
		V value = unmapNullValue(mappedValue);
		
		if (value == null && (value = mappingFunction.apply(keyPartA, keyPartB)) != null) {
			values[index] = value;
			if (mappedValue == null) {
				keys[index] = key;
				if (++size > resizeThreshold) {
					increaseCapacity();
				}
			}
		}
		return value;
	}
	
	public V computeIfPresent(
			final int keyPartA,
			final int keyPartB,
			final EntryRemap<? super V, ? extends V> remappingFunction) {
		final long key = compoundKey(keyPartA, keyPartB);
		requireNonNull(remappingFunction);
		final long[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object mappedValue;
		while (null != (mappedValue = values[index])) {
			if (key == keys[index]) {
				break;
			}
			index = ++index & mask;
		}
		
		V value = unmapNullValue(mappedValue);
		
		if (value != null) {
			value = remappingFunction.apply(keyPartA, keyPartB, value);
			values[index] = value;
			if (value == null) {
				--size;
				compactChain(index);
			}
		}
		return value;
	}
	
	public V compute(final int keyPartA, final int keyPartB, final EntryRemap<? super V, ? extends V> remappingFunction) {
		final long key = compoundKey(keyPartA, keyPartB);
		requireNonNull(remappingFunction);
		final long[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object mappedValue;
		while ((mappedValue = values[index]) != null) {
			if (key == keys[index]) {
				break;
			} 
			
			index = ++index & mask;
		}
		
		final V newValue = remappingFunction.apply(keyPartA, keyPartB, unmapNullValue(mappedValue));
		if (newValue != null) {
			values[index] = newValue;
			if (mappedValue == null) {
				keys[index] = key;
				if (++size > resizeThreshold) {
					increaseCapacity();
				}
			}
		} else if (null != mappedValue) {
			values[index] = null;
			size --;
			compactChain(index);
		}
		
		return newValue;
	}
	
	public V merge(
			final int keyPartA, 
			final int keyPartB, 
			final V value, 
			final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		final long key = compoundKey(keyPartA, keyPartB);
		requireNonNull(value);
		requireNonNull(remappingFunction);
		final long[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object mappedValue;
		while (null != (mappedValue = values[index])) {
			if (key == keys[index]) {
				break;
			}
			index = ++index & mask;
		}
		
		final V oldValue = unmapNullValue(mappedValue);
		final V newValue = null == oldValue ? value : remappingFunction.apply(oldValue, value);
		
		if (null != newValue) {
			values[index] = newValue;
			if (null == mappedValue) {
				keys[index] = key;
				if (++size > resizeThreshold) {
					increaseCapacity();
				}
			}
		} else if (null != mappedValue) {
			values[index] = null;
			size --;
			compactChain(index);
		}
		
		return newValue;
	}
	
	@SuppressWarnings("unchecked")
	public void forEach(final Consumer<V> consumer) {
		int remaining = this.size;
		final Object[] values = this.values;
		
		for (int i = 0, length = values.length; remaining > 0 && i < length; i++) {
			final Object value = values[i];
			if (value != null) {
				consumer.accept((V) value);
				--remaining;
			}
		}
	}
	
	public void forEach(final EntryConsumer<V> consumer) {
		int remaining = this.size;
		final long[] keys = this.keys;
		final Object[] values = this.values;
		
		for (int i = 0, length = values.length; remaining > 0 && i < length; i ++) {
			final Object value = values[i];
			if (null != value) {
				final long compoundKey = keys[i];
				final int keyPartA = (int) (compoundKey >>> 32);
				final int keyPartB = (int) (compoundKey & 0xFFFF_FFFFL);
				
				consumer.accept(keyPartA, keyPartB, unmapNullValue(value));
				--remaining;
			} 
		}
	}
	
	@SuppressWarnings("unchecked")
	public V replace(final int keyPartA, final int keyPartB, final V value) {
		final long key = compoundKey(keyPartA, keyPartB);
		final V val = (V)mapNullValue(value);
		requireNonNull(val, "value cannot be null");
		
		final long[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object oldValue;
		while (null != (oldValue = values[index])) {
			if (key == keys[index]) {
				values[index] = val;
				break;
			}
			
			index = ++index & mask;
		}
		
		return unmapNullValue(oldValue);
	}
	
	@SuppressWarnings("unchecked")
	public boolean replace(final int keyPartA, final int keyPartB, final V oldValue, final V newValue) {
		final long key = compoundKey(keyPartA, keyPartB);
		final V val = (V)mapNullValue(newValue);
		requireNonNull(val, "value cannot be null");
		
		final long[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object mappedValue;
		while (null != (mappedValue = values[index])) {
			if (key == keys[index]) {
				if (Objects.equals(unmapNullValue(mappedValue), oldValue)) {
					values[index] = val;
					return true;
				}
				break;
			}
			
			index = ++index & mask;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public V putIfAbsent(final int keyPartA, final int keyPartB, final V value) {
		final long key = compoundKey(keyPartA, keyPartB);
		final V val = (V) mapNullValue(value);
		requireNonNull(val, "value cannot be null");
		
		final long[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object mappedValue;
		while (null != (mappedValue = values[index])) {
			if (key == keys[index]) {
				break;
			}
			
			index = ++index & mask;
		}
		
		final V oldValue = unmapNullValue(mappedValue);
		if (null == oldValue) {
			if (null == mappedValue) {
				++size;
				keys[index] = key;
			}
			
			values[index] = val;
			if (size > resizeThreshold) {
				increaseCapacity();
			}
		}
		
		return oldValue;
	}
	
	public boolean remove(final int keyPartA, final int keyPartB, final V value) {
		final long key = compoundKey(keyPartA, keyPartB);
		final Object val = mapNullValue(value);
		if (val != null) {
			final long[] keys = this.keys;
			final Object[] values = this.values;
			final int mask = values.length - 1;
			int index = Hashing.hash(key, mask);
			
			Object mappedValue;
			while (null != (mappedValue = values[index])) {
				if (key == keys[index]) {
					if (Objects.equals(unmapNullValue(mappedValue), value)) {
						values[index] = null;
						--size;
						
						compactChain(index);
						return true;
					}
					break;
				}
				
				index = ++index & mask;
			}
		}
		return false;
	}
	
	public int size() {
		return size;
	}
	
	public boolean isEmpty() {
		return 0 == size;
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append('{');
		
		final long[] keys = this.keys;
		final Object[] values = this.values;
		for (int i = 0, size = values.length; i < size; i++) {
			final Object value = values[i];
			if (value != null) {
				final long compoundKey = keys[i];
				final int keyPartA = (int) (compoundKey >>> 32);
				final int keyPartB = (int) (compoundKey & 0xFFFF_FFFFL);
				
				sb.append(keyPartA).append("_").append(keyPartB).append('=').append(value).append(", ");
			}
		}
		if (sb.length() > 1) {
			sb.setLength(sb.length() - 2);
		}
		
		sb.append('}');
		return sb.toString();
	}
	
	private void rehash(final int newCapacity) {
		final int mask = newCapacity - 1;
		resizeThreshold = (int)(newCapacity * loadFactor);
		
		final long[] tempKeys = new long[newCapacity];
		final Object[] tempValues = new Object[newCapacity];
		
		final long[] keys = this.keys;
		final Object[] values = this.values;
		
		for (int i = 0, size = values.length; i < size; i++) {
			final Object value = values[i];
			if (value != null) {
				final long key = keys[i];
				int newHash = Hashing.hash(key, mask);
				
				while (null != tempValues[newHash]) {
					newHash = ++newHash & mask;
				}
				
				tempKeys[newHash] = key;
				tempValues[newHash] = value;
			}
		}
		
		this.keys = tempKeys;
		this.values = tempValues;
	}

	@SuppressWarnings("FinalParameters")
	private void compactChain(int deleteIndex) {
		final int mask = values.length - 1;
		int index = deleteIndex;
		final long[] keys = this.keys;
		final Object[] values = this.values;
		
		while (true) {
			index = ++index & mask;
			final Object value = values[index];
			if (value == null) {
				break;
			}
			
			final long key = keys[index];
			final int hash = Hashing.hash(key, mask);
			
			if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
					(hash <= deleteIndex && deleteIndex <= index)) {
				keys[deleteIndex] = key;
				values[deleteIndex] = value;
				
				values[index] = null;
				deleteIndex = index;
			}		
		}
	}
	
	private void increaseCapacity() {
		final int newCapacity = values.length << 1;
		if (newCapacity < 0) {
			throw new IllegalStateException("max capacity reached at size=" + size);
		}
		rehash(newCapacity);
	}
}
