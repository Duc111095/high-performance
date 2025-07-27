package com.ducnh.highperformance.collections;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import com.ducnh.highperformance.BitUtil;

public final class CollectionUtil {
	private CollectionUtil() {}
	
	public static <K, V> V getOrDefault(final Map<K, V> map, final K key, final Function<K, V> supplier) {
		V value = map.get(key);
		if (value == null) {
			value = supplier.apply(key);
			map.put(key, value);
		}
		return value;
	}
	
	public static <V> int sum(final List<V> values, final ToIntFunction<V> function) {
		int total = 0;
		final int size = values.size();
		
		for (int i = 0; i < size; i++) {
			final V value = values.get(i);
			total += function.applyAsInt(value);
		}
		return total;
	}
	
	public static void validateLoadFactor(final float loadFactor) {
		if (loadFactor < 0.1f || loadFactor > 0.9f) {
			throw new IllegalArgumentException("load factor must be in the range of 0.1 to 0.9: " + loadFactor);
		}
	}
	
	public static void validatePositivePowerOfTwo(final int value) {
		if (!BitUtil.isPowerOfTwo(value)) {
			throw new IllegalArgumentException("value must be a positive power of two: " + value);
		}
	}
	
	public static <T> int removeIf(final List<T> values, final Predicate<T> predicate) {
		int size = values.size();
		int total = 0;
		
		for (int i = 0; i < size; ) {
			 final T value = values.get(i);
			 if (predicate.test(value)) {
				 values.remove(i);
				 total++;
				 size--;
			 } else {
				 i++;
			 }
		}
		return total;
	}
}
