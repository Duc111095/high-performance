package com.ducnh.highperformance.collections;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;

import static com.ducnh.highperformance.BitUtil.findNextPositivePowerOfTwo;
import static com.ducnh.highperformance.collections.CollectionUtil.validateLoadFactor;

public class Object2ObjectHashMap<K, V> implements Map<K, V> {
	static final int MIN_CAPACITY = 8;
	private final float loadFactor;
	private int resizeThreshold;
	private int size = 0;
	private final boolean shouldAvoidAllocation;
	
	private Object[] entries;
	private KeySet keySet;
	private ValueCollection valueCollection;
	private EntrySet entrySet;
	
	public Object2ObjectHashMap() {
		this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR);
	}
	
	public Object2ObjectHashMap(final int initialCapacity, final float loadFactor) {
		this(initialCapacity, loadFactor, true);
	}
	
	public Object2ObjectHashMap(final int initialCapacity, final float loadFactor, final boolean shouldAvoidAllocation) {
		validateLoadFactor(loadFactor);
		
		this.loadFactor = loadFactor;
		this.shouldAvoidAllocation = shouldAvoidAllocation;
		
		capacity(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity)));
	}
	
	public Object2ObjectHashMap(final Object2ObjectHashMap<K, V> mapToCopy) {
		this.loadFactor = mapToCopy.loadFactor;
		this.resizeThreshold = mapToCopy.resizeThreshold;
		this.size = mapToCopy.size;
		this.shouldAvoidAllocation = mapToCopy.shouldAvoidAllocation;
		
		entries = mapToCopy.entries.clone();
	}
	
	public float loadFactor() {
		return loadFactor;
	}
	
	public int resizeThreshold() {
		return resizeThreshold;
	}
	
	public int capacity() {
		return entries.length >> 1;
	}
	
	public int size() {
		return size;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public V get(final Object key) {
		return unmapNullValue(getMapped(key));
	}
	
	@SuppressWarnings("unchecked")
	private V getMapped(final Object key) {
		requireNonNull(key);
		
		final Object[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = Hashing.evenHash(key.hashCode(), mask);
		
		Object value;
		while (null != (value = entries[keyIndex + 1])) {
			if (Objects.equals(entries[keyIndex], key)) {
				break;
			}
			
			keyIndex = next(keyIndex, mask);
		}
		
		return (V)value;
	}
	
	public V put(final K key, final V value) {
		final Object val = mapNullValue(value);
		requireNonNull(val, "value cannot be null");
		
		final Object[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = Hashing.evenHash(key.hashCode(), mask);
		
		Object oldValue;
		
		while(null != (oldValue = entries[keyIndex + 1]) {
			if (Objects.equals(entries[keyIndex], key)) {
				break;
			}
			
			keyIndex = next(keyIndex, mask);
		}
		
		if (null == oldValue) {
			++size;
			entries[keyIndex] = key;
		}
		
		entries[keyIndex + 1] = val;
		
		increaseCapacity();
		
		return unmapNullValue(oldValue);
	}
	
	private void increaseCapacity() {
		if (size > resizeThreshold) {
			final int newCapacity = entries.length;
			rehash(newCapacity);
		}
	}
	
	private void rehash(final int newCapacity) {
		final Object[] oldEntries = this.entries;
		final int length = entries.length;
		
		capacity(newCapacity);
		
		final Object[] newEntries = entries;
		final int mask = entries.length - 1;
		
		for (int keyIndex = 0; keyIndex < length; keyIndex += 2) {
			final Object value = oldEntries[keyIndex + 1];
			if (null != value) {
				final Object key = oldEntries[keyIndex];
				int index  = Hashing.evenHash(key.hashCode(), mask);
				
				while (null != newEntries[index + 1]) {
					index = next(index, mask);
				}
				
				newEntries[index] = key;
				newEntries[index + 1] = value;
			}
		}
	}
	
	public boolean containsValue(final Object value) {
		final Object val = mapNullValue(value);
		boolean found = false;
		if (null != val) {
			final Object[] entries = this.entries;
			final int length = entries.length;
			
			for (int valueIndex = 1; valueIndex < length; valueIndex += 2) {
				final Object entry = entries[valueIndex];
				if (null != entry && Objects.equals(val, entry)) {
					found = true;
					break;
				}
			}
		}
		
		return found;
	}
	
	public void clear() {
		if (size > 0) {
			Arrays.fill(entries, null);
			size = 0;
		}
	}
	
	public void compact() {
		final int idealCapacity = (int) Math.round(size() * (1.0d / loadFactor));
		rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
	}
	
	@SuppressWarnings("unchecked")
	public void forEach(final BiConsumer<? super K, ? super V> consumer) {
		int remaining = size;
		final Object[] entries = this.entries;
		
		for (int i = 1, length = entries.length; remaining > 0 && i < length; i += 2) {
			final Object value = entries[i];
			if (value != null) {
				consumer.accept((K) entries[i - 1], unmapNullValue(value));
				--remaining;
			}
		}
	}
	
	public boolean containsKey(final Object key) {
		return null != getMapped(key);
	}
	
	public void putAll(final Map<? extends K, ? extends V> map) {
		for (final Entry<? extends K, ? extends V> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}
	
	public KeySet keySet() {
		if (null == keySet) {
			keySet = new KeySet();
		}
		
		return keySet;
	}
	
	public ValueCollection values() {
		if (null == valueCollection) {
			valueCollection = new ValueCollection();
		}
		
		return valueCollection;
	}
	
	public EntrySet entrySet() {
		if (null == entrySet) {
			entrySet = new EntrySet();
		}
		
		return entrySet;
	}
	
	public V remove(final Object key) {
		final Object[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = Hashing.evenHash(key.hashCode(), mask);
		
		Object value;
		while (null != (value = entries[keyIndex + 1])) {
			if (Objects.equals(entries[keyIndex], key)) {
				entries[keyIndex] = null;
				entries[keyIndex + 1] = null;
				size--;
				
				compactChain(keyIndex);
				break;
			}
			
			keyIndex = next(keyIndex, mask);
		}
		
		return unmapNullValue(value);
	}
	
	@SuppressWarnings("FinalParameters")
	private void compactChain(int deleteKeyIndex) {
		final Object[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = deleteKeyIndex;
		
		while (true) {
			keyIndex = next(keyIndex, mask);
			final Object value = entries[keyIndex + 1];
			if (null == value) {
				break;
			}
			
			final Object key = entries[keyIndex];
			final int hash = Hashing.evenHash(key.hashCode(), mask);
			
			if ((keyIndex < hash && (hash <= deleteKeyIndex || deleteKeyIndex <= keyIndex)) || 
					(hash <= deleteKeyIndex && deleteKeyIndex <= keyIndex)) {
				entries[deleteKeyIndex] = key;
				entries[deleteKeyIndex + 1] = value;
				entries[keyIndex] = null;
				entries[keyIndex + 1] = null;
				deleteKeyIndex = keyIndex;
			}
		}
	}
	
	public String toString() {
		if (isEmpty()) {
			return "{}";
		}
		
		final EntryIterator entryIterator = new EntryIterator();
		entryIterator.reset();
		
		final StringBuilder sb = new StringBuilder().append('{');
		
		while (true) {
			entryIterator.next();
			sb.append(entryIterator.getKey()).append('=').append(unmapNullValue(entryIterator.getValue()));
			if (!entryIterator.hasNext()) {
				return sb.append('}').toString();
			}
			sb.append(',').append(' ');
		}
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		
		if (!(o instanceof Map)) {
			return false;
		}
		
		final Map<K, V> that = (Map<K, V>) o;
		
		return size == that.size() && entrySet().equals(that.entrySet());
	}
	
	public int hashCode() {
		return entrySet().hashCode();
	}
	
	public V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) {
		final Object[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = Hashing.evenHash(key.hashCode(), mask);
		
		Object mappedValue;
		while (null != (mappedValue = entries[keyIndex + 1])) {
			if (Objects.equals(entries[keyIndex], key)) {
				break;
			}
			
			keyIndex = next(keyIndex, mask);
		}
		
		V value = unmapNullValue(mappedValue);
		if (value == null && (value = mappingFunction.apply(key)) != null) {
			entries[keyIndex + 1] = value;
			if (mappedValue == null) {
				entries[keyIndex] = key;
				++size;
				increaseCapacity();
			}
		}
		
		return value;
	}
	
	@Override
	public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		final Object[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = Hashing.evenHash(key.hashCode(), mask);
		
		Object mappedValue;
		while (null != (mappedValue = entries[keyIndex + 1])) {
			if (Objects.equals(entries[keyIndex], key)) {
				break;
			}
			
			keyIndex = next(keyIndex, mask);
		}
		
		V value = unmapNullValue(mappedValue);
		if (value != null) {
			value = remappingFunction.apply(key, value);
			entries[keyIndex + 1] = value;
			if (value == null) {
				entries[keyIndex] = null;
				size--;
				compactChain(keyIndex);
			}
		}
		
		return value;
	}
	
	@Override
	public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		final Object[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = Hashing.evenHash(key.hashCode(), mask);
		
		Object mappedValue;
		while (null != (mappedValue = entries[keyIndex + 1])) {
			if (Objects.equals(entries[keyIndex], key)) {
				break;
			}
			
			keyIndex = next(keyIndex, mask);
		}
		
		final V oldValue = unmapNullValue(mappedValue);
		final V newValue = remappingFunction.apply(key, oldValue);
		
		if (newValue != null) {
			entries[keyIndex + 1] = newValue;
			if (mappedValue == null) {
				entries[keyIndex] = key;
				size++;
				increaseCapacity();
			}
		} else if (mappedValue != null) {
			entries[keyIndex] = null;
			entries[keyIndex + 1] = null;
			size--;
			compactChain(keyIndex);
		}
		
		return newValue;
	}
	
	protected Object mapNullValue(final Object value) {
		return value;
	}
	
	@SuppressWarnings("unchecked")
	protected V unmapNullValue(final Object value) {
		return (V)value;
	}
	
	private static int next(final int index, final int mask) {
		return (index + 2) & mask;
	}
	
	private void capacity(final int newCapacity) {
		final int entriesLength = newCapacity * 2;
		if (entriesLength < 0) {
			throw new IllegalStateException("max capacity reached at size=" + size);
		}
		
		resizeThreshold = (int) (newCapacity * loadFactor);
		entries = new Object[entriesLength];
	}
	
	abstract class AbstractIterator {
		
		protected boolean isPositionValid = false;
		private int remaining;
		private int positionCounter;
		private int stopCounter;
		
		final void reset() {
			isPositionValid = false;
			remaining = Object2ObjectHashMap.this.size;
			final Object[] entries = Object2ObjectHashMap.this.entries;
			final int capacity = entries.length;
			
			int keyIndex = capacity;
			if (null != entries[capacity - 1]) {
				for (int  i = 1; i < capacity; i += 2) {
					if (null == entries[i]) {
						keyIndex = i - 1;
						break;
					}
				}
			}
			
			stopCounter = keyIndex;
			positionCounter = keyIndex + capacity;
		}
		
		final int keyPosition() {
			return positionCounter & entries.length - 1;
		}
		
		public int remaining() {
			return remaining;
		}
		
		public boolean hasNext() {
			return remaining > 0;
		}
		
		protected final void findNext() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			
			final Object[] entries = Object2ObjectHashMap.this.entries;
			final int mask = entries.length - 1;
			
			for (int keyIndex = positionCounter - 2; keyIndex >= stopCounter; keyIndex -= 2) {
				final int index = keyIndex & mask;
				if (null != entries[index + 1]) {
					isPositionValid = true;
					positionCounter = keyIndex;
					--remaining;
					return;
				}
			}
			
			isPositionValid = false;
			throw new IllegalStateException();
		}
		
		public void remove() {
			if (isPositionValid) {
				final int position = keyPosition();
				final Object[] entries = Object2ObjectHashMap.this.entries;
				entries[position] = null;
				entries[position + 1] = null;
				--size;
				
				compactChain(position);
				isPositionValid = false;
			}
			else {
				throw new IllegalStateException();
			}
		}
	}
	
	public final class KeyIterator extends AbstractIterator implements Iterator<K> {
		public KeyIterator() {
			
		}
		
		@SuppressWarnings("unchecked")
		public K next() {
			findNext();
			return (K)entries[keyPosition()];
		}
	}
	
	public final class ValueIterator extends AbstractIterator implements Iterator<V> {
		public ValueIterator() {
			
		}
		
		public V next() {
			findNext();
			return unmapNullValue(entries[keyPosition() + 1]);
		}
	}
	
	public final class EntryIterator extends AbstractIterator
		implements Iterator<Entry<K, V>>, Entry<K, V> {
		
		public EntryIterator() {
			
		}
		
		@SuppressWarnings("unchecked")
		public K getKey() {
			return (K)entries[keyPosition()];
		}
		
		public V getValue() {
			return unmapNullValue(entries[keyPosition() + 1]);
		}
		
		@SuppressWarnings("unchecked")
		public V setValue(final V value) {
			final V val = (V) mapNullValue(value);
			
			if (!isPositionValid) {
				throw new IllegalStateException();
			}
			
			if (null == val) {
				throw new IllegalArgumentException();
			}
			
			final int keyPosition = keyPosition();
			final Object[] entries = Object2ObjectHashMap.this.entries;
			final Object prevValue = entries[keyPosition + 1];
			entries[keyPosition + 1] = val;
			
			return unmapNullValue(prevValue);
		}
		
		public Entry<K, V> next() {
			findNext();
			
			if (shouldAvoidAllocation) {
				return this;
			}
			
			return allocateDuplicateEntry();
		}
		
		private Entry<K, V> allocateDuplicateEntry() {
			return new MapEntry(getKey(), getValue());
		}
		
		public int hashCode() {
			return getKey().hashCode() ^ Objects.hashCode(getValue());
		}
		
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			
			if (!(o instanceof Entry)) {
				return false;
			}
			
			final Entry<?, ?> that = (Entry<?, ?>)o;
			
			return Objects.equals(getKey(), that.getKey()) && Objects.equals(getValue(), that.getValue());
		}
		
		public final class MapEntry implements Entry<K, V> {
			private final K k;
			private V v;
			
			public MapEntry(final K k, final V v) {
				this.k = k;
				this.v = v;
			}
			
			public K getKey() {
				return k;
			}
			
			public V getValue() {
				return v;
			}
			
			public V setValue(final V value) {
				final V oldValue = Object2ObjectHashMap.this.put(k, value);
				v = value;
				return oldValue;
			}
			
			public int hashCode() {
				return k.hashCode() ^ v.hashCode();
			}
			
			public boolean equals(final Object o) {
				if (this == o) {
					return true;
				}
				
				if (!(o instanceof Entry)) {
					return false;
				}
				
				final Entry<?, ?> e = (Entry<?, ?>) o;
				return Objects.equals(k, e.getKey()) && Objects.equals(v, e.getValue());
			}
			
			public String toString() {
				return k + "=" + v;
			}
		}
	}
	
	public final class KeySet extends AbstractSet<K> {
		private final KeyIterator keyIterator = shouldAvoidAllocation ? new KeyIterator() : null;
		
		public KeySet() {
			
		}
		
		public KeyIterator iterator() {
			KeyIterator keyIterator = this.keyIterator;
			if (null == keyIterator) {
				keyIterator = new KeyIterator();
			}
			
			keyIterator.reset();
			return keyIterator;
		}
		
		public int size() {
			return Object2ObjectHashMap.this.size();
		}
		
		public boolean isEmpty() {
			return Object2ObjectHashMap.this.isEmpty();
		}
		
		public void clear() {
			Object2ObjectHashMap.this.clear();
		}
		
		public boolean contains(final Object o) {
			return containsKey(o);
		}
		
		@SuppressWarnings("unchecked")
		public void forEach(final Consumer<? super K> action) {
			int remaining = size;
			final Object[] entries = Object2ObjectHashMap.this.entries;
			
			for (int i = 1, length = entries.length; remaining > 0 && i < length; i += 2) {
				if (null != entries[i]) {
					action.accept((K)entries[i - 1]);
					--remaining;
				}
			}
		}
	}
	
	public final class ValueCollection extends AbstractCollection<V> {
		private final ValueIterator valueIterator = shouldAvoidAllocation ? new ValueIterator() : null;
		
		public ValueCollection() {
			
		}
		
		public ValueIterator iterator() {
			ValueIterator valueIterator = this.valueIterator;
			if (null == valueIterator) {
				valueIterator = new ValueIterator();
			}
			
			valueIterator.reset();
			return valueIterator;
		}
		
		public int size() {
			return Object2ObjectHashMap.this.size();
		}
		
		public boolean contains(final Object o) {
			return containsValue(o);
		}
		
		public void forEach(final Consumer<? super V> action) {
			int remaining = size;
			final Object[] entries = Object2ObjectHashMap.this.entries;
			
			for (int i = 1, length = entries.length; remaining > 0 && i < length; i += 2) {
				final Object entry = entries[i];
				if (null != entry) {
					action.accept(unmapNullValue(entry));
					--remaining;
				}
			}
 		}
	}
	
	public final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		private final EntryIterator entryIterator = shouldAvoidAllocation ? new EntryIterator() : null;
	
		public EntrySet() {
			
		}
		
		public EntryIterator iterator() {
			EntryIterator entryIterator = this.entryIterator;
			if (null == entryIterator) {
				entryIterator = new EntryIterator();
			}
			
			entryIterator.reset();
			return entryIterator;
		}
		
		public int size() {
			return Object2ObjectHashMap.this.size();
		}
		
		public boolean isEmpty() {
			return Object2ObjectHashMap.this.isEmpty();
		}
		
		public void clear() {
			Object2ObjectHashMap.this.clear();
		}
		
		public boolean contains(final Object o) {
			if (!(o instanceof Entry)) {
				return false;
			}
			
			final Entry<?, ?> entry = (Entry<?, ?>)o;
			final V value = getMapped(entry.getKey());
			return null != value && Objects.equals(value, mapNullValue(entry.getValue()));
		}
		
		public Object[] toArray() {
			return toArray(new Object[size()]);
		}
		
		@SuppressWarnings("unchecked")
		public <T> T[] toArray(final T[] a) {
			final T[] array = a.length >= size ? 
					a : (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
			final EntryIterator it = iterator();
			
			for (int i = 0; i < array.length; i++) {
				if (it.hasNext()) {
					it.next();
					array[i] = (T)it.allocateDuplicateEntry();
				} else {
					array[i] = null;
					break;
				}
			}
			
			 return array;
		}
	}
}
