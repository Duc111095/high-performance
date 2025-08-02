package com.ducnh.highperformance.collections;

import static java.util.Objects.requireNonNull;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

import static com.ducnh.highperformance.BitUtil.findNextPositivePowerOfTwo;
import static com.ducnh.highperformance.collections.CollectionUtil.validateLoadFactor;

public class Object2IntHashMap<K> {

	static final int MIN_CAPACITY = 0;
	
	private final float loadFactor;
	private final int missingValue;
	private int resizeThreshold;
	private int size;
	private final boolean shouldAvoidAllocation;
	
	private K[] keys;
	private int[] values;
	
	private ValueCollection valueCollection;
	private KeySet keySet;
	private EntrySet entrySet;
	
	public Object2IntHashMap(final int missingValue) {
		this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, missingValue);
	}
	
	public Object2IntHashMap(
			final int initialCapacity,
			final float loadFactor,
			final int missingValue) {
		this(initialCapacity, loadFactor, missingValue, true);
	}
	
	@SuppressWarnings("unchecked")
	public Object2IntHashMap(
			final int initialCapacity,
			final float loadFactor,
			final int missingValue,
			final boolean shouldAvoidAllocation) {
		validateLoadFactor(loadFactor);
		
		this.loadFactor = loadFactor;
		final int capacity = findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity));
		resizeThreshold = (int)(capacity * loadFactor);
		
		this.missingValue = missingValue;
		this.shouldAvoidAllocation = shouldAvoidAllocation;
		keys = (K[]) new Object[capacity];
		values = new int[capacity];
		Arrays.fill(values, missingValue);
	}
	
	public Object2IntHashMap(final Object2IntHashMap<K> mapToCopy) {
		this.loadFactor = mapToCopy.loadFactor;
		this.resizeThreshold = mapToCopy.resizeThreshold;
		this.size = mapToCopy.size;
		this.missingValue = mapToCopy.missingValue;
		this.shouldAvoidAllocation = mapToCopy.shouldAvoidAllocation;
		
		keys = mapToCopy.keys.clone();
		values = mapToCopy.values.clone();
	}
	
	public int missingValue() {
		return missingValue;
	}
	
	public float loadFactor() {
		return loadFactor;
	}
	
	public int capacity() {
		return values.length;
	}
	
	public int resizeThreshold() {
		return resizeThreshold;
	}
	
	public int size() {
		return size;
	}
	
	public boolean isEmpty() {
		return 0 == size;
	}
	
	@SuppressWarnings("unchecked")
	public boolean containsKey(final Object key) {
		return missingValue != getValue((K) key);
	}
	
	public boolean containsValue(final Object value) {
		return containsValue((int) value);
	}
	
	public boolean containsValue(final int value) {
		if (missingValue == value) {
			return false;
		}
		
		boolean found = false;
		final int[] values = this.values;
		for (final int v : values) {
			if (value == v) {
				found = true;
				break;
			}
		}
		
		return found;
	}
	
	@SuppressWarnings("unchecked")
	public int getOrDefault(final Object key, final int defaultValue) {
		final int value = getValue((K) key);
		return missingValue == value ? defaultValue : value;
	}
	
	@SuppressWarnings("unchecked")
	public Integer get(final Object key) {
		return valueOrNull(getValue((K) key));
	}
	
	public int getValue(final K key) {
		requireNonNull(key);
		final int missingValue = this.missingValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int value;
		while (missingValue != (value = values[index])) {
			if (Objects.equals(keys[index], key)) {
				break;
			}
			
			index = ++index & mask;
		}
		
		return value;
	}
	
	@SuppressWarnings("overloads")
	public int computeIfAbsent(final K key, final ToIntFunction<? super K> mappingFunction) {
		requireNonNull(key);
		final int missingValue = this.missingValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int value;
		while (missingValue != (value = values[index])) {
			if (Objects.equals(keys[index], key)) {
				return value;
			}
			
			index = ++index & mask;
		}
		
		final int newValue = mappingFunction.applyAsInt(key);
		if (missingValue != newValue) {
			keys[index] = key;
			values[index] = newValue;
			++size;
			increaseCapacity();
		}
		
		return newValue;
	}
	
	@SuppressWarnings("overloads")
	public int computeIfPresent(final K key, final ObjectIntToIntFunction<? super K> remappingFunction) {
		requireNonNull(key);
		final int missingValue = this.missingValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int value;
		while (missingValue != (value = values[index])) {
			if (Objects.equals(keys[index], key)) {
				final int newValue = remappingFunction.apply(key, value);
				values[index] = newValue;
				
				if (missingValue == newValue) {
					keys[index] = null;
					size--;
					compactChain(index);
				}
				
				return newValue;
			}
			
			index = ++index & mask;
		}
		
		return missingValue;
	}
	
	@SuppressWarnings("overloads")
	public int compute(final K key, final ObjectIntToIntFunction<? super K> remappingFunction) {
		requireNonNull(key);
		final int missingValue = this.missingValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int oldValue;
		while (missingValue != (oldValue = values[index])) {
			if (Objects.equals(keys[index], key)) {
				break;
			}
			
			index = ++index & mask;
		}
		
		final int newValue = remappingFunction.apply(key, oldValue);
		if (missingValue != newValue) {
			values[index] = newValue;
			if (missingValue == oldValue) {
				keys[index] = key;
				++size;
				increaseCapacity();
			}
		}
		else if (missingValue != oldValue) {
			keys[index] = null;
			values[index] = missingValue;
			--size;
			compactChain(index);
		}
		
		return newValue;
	}
	
	public int merge(final K key, final int value, final IntIntFunction remappingFunction) {
		requireNonNull(key);
		requireNonNull(remappingFunction);
		final int missingValue = this.missingValue;
		if (missingValue == value) {
			throw new IllegalArgumentException("cannot accept missingValue");
		}
		
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int oldValue;
		while (missingValue != (oldValue = values[index])) {
			if (Objects.equals(keys[index], key)) {
				break;
			}
			
			index = ++index & mask;
		}
		
		final int newValue = missingValue == oldValue ? value : remappingFunction.apply(oldValue, value);
		if (missingValue != newValue) {
			keys[index] = key;
			values[index] = value;
			if (missingValue == oldValue) {
				keys[index] = key;
				++size;
				increaseCapacity();
			}
		} else {
			keys[index] = null;
			values[index] = missingValue;
			--size;
			compactChain(index);
		}
		
		return newValue;
	}
	
	public Integer put(final K key, final Integer value) {
		return valueOrNull(put(key, (int) value));
	}
	
	public int put(final K key, final int value) {
		requireNonNull(key);
		final int missingValue = this.missingValue;
		if (missingValue == value) {
			throw new IllegalArgumentException("cannot accept missingValue");
		}
		
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int oldValue;
		while (missingValue != (oldValue = values[index])) {
			if (Objects.equals(keys[index], key)) {
				break;
			}
			
			index = ++index & mask;
		}
		
		if (missingValue == oldValue) {
			++size;
			keys[index] = key;
		}
		
		values[index] = value;
		
		increaseCapacity();
		
		return oldValue;
	}
	
	public Integer putIfAbsent(final K key, final Integer value) {
		return valueOrNull(putIfAbsent(key, (int) value));
	}
	
	public int putIfAbsent(final K key, final int value) {
		requireNonNull(key);
		final int missingValue = this.missingValue;
		if (missingValue == value) {
			throw new IllegalArgumentException("cannot accept missingValue");
		}
		
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
	
		int oldValue;
		while (missingValue != (oldValue = values[index])) {
			if (Objects.equals(keys[index], key)) {
				return oldValue;
			}
			
			index = ++index & mask;
		}
		
		keys[index] = key;
		values[index] = value;
		
		++size;
		increaseCapacity();
		
		return missingValue;
	}
	
	public boolean remove(final Object key, final Object value) {
		return remove(key, (int)value);
	}
	
	public boolean remove(final Object key, final int value) {
		final int missingValue = this.missingValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int existingValue;
		while (missingValue != (existingValue = values[index])) {
			if (Objects.equals(keys[index], key)) {
				if (value == existingValue) {
					keys[index] = null;
					values[index] = missingValue;
					--size;
					
					compactChain(index);
					return true;
				}
				
				break;
			}
			
			index = ++index & mask;
		}
		
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public Integer remove(final Object key) {
		return valueOrNull(removeKey((K)key));
	}
	
	public int removeKey(final K key) {
		requireNonNull(key);
		final int missingValue = this.missingValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int value;
		while (missingValue != (value = values[index])) {
			if (Objects.equals(keys[index], key)) {
				keys[index] = null;
				values[index] = missingValue;
				--size;
				
				compactChain(index);
				break;
			}
			
			index = ++index & mask;
		}
		
		return value;
	}
	
	public void clear() {
		if (size > 0) {
			Arrays.fill(keys, null);
			Arrays.fill(values, missingValue);
			size = 0;
		}
	}
	
	public void compact() {
		final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
		rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
	}
	
	public void putAll(final Map<? extends K, ? extends Integer> map) {
		for (final Entry<? extends K, ? extends Integer> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}
	
	public void putAll(final Object2IntHashMap<? extends K>  map) {
		final int missingValue = map.missingValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int length = values.length;
		for (int index = 0, remaining = map.size; remaining > 0 && index < length; index ++) {
			final int value = values[index];
			if (missingValue != value) {
				put(keys[index], value);
				remaining--;
			}
		}
	}
	
	public KeySet keySet() {
		if (null == keySet) {
			keySet = new KeySet();
		}
		
		return keySet;
	}
	
	public ValueCollection valueCollection() {
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
	
	public String toString() {
		if (isEmpty()) {
			return "{}";
		}
		
		final EntryIterator entryIterator = new EntryIterator();
		entryIterator.reset();
		
		final StringBuilder sb = new StringBuilder().append('{');
		while (true) {
			entryIterator.next();
			sb.append(entryIterator.getKey()).append('=').append(entryIterator.getIntValue());
			if (!entryIterator.hasNext()) {
				return sb.append('}').toString();
			}
			sb.append(',').append(' ');
		}
	}
	
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		
		if (!(o instanceof Map)) {
			return false;
		}
		
		final Map<?, ?> that = (Map<?, ?>)o;
		
		if (size != that.size()) {
			return false;
		}
		
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int missingValue = this.missingValue;
		final int thatMissingValue = o instanceof Object2IntHashMap ? ((Object2IntHashMap<?>)o).missingValue : missingValue;
		for (int i = 0, length = values.length; i < length; i++) {
			final int thisValue = values[i];
			if (missingValue !=  thisValue) {
				final Object thatValueObject = that.get(keys[i]);
				if (!(thatValueObject instanceof Integer)) {
					return false;
				}
				
				final int thatValue = (Integer) thatValueObject;
				if (thatMissingValue == thatValue || thisValue != thatValue) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public int hashCode() {
		int result = 0;
		final K[] keys = this.keys;
		final int[] values = this.values;
		for (int i = 0, length = values.length; i < length; i++) {
			final int value = values[i];
			if (missingValue != value) {
				result += (keys[i].hashCode() ^ Integer.hashCode(value));
			}
		}
		
		return result;
	}
	
	public int replace(final K key, final int value) {
		requireNonNull(key);
		final int missingValue = this.missingValue;
		if (missingValue == value) {
			throw new IllegalArgumentException("cannot accept missingValue");
		}
		
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int existingValue;
		while (missingValue != (existingValue = values[index])) {
			if (Objects.equals(keys[index], key)) {
				values[index] = value;
				return existingValue;
			}
			
			index = ++index & mask;
		}
		
		return missingValue;
	}
	
	public boolean replace(final K key, final int oldValue, final int newValue) {
		requireNonNull(key);
		final int missingValue = this.missingValue;
		if (missingValue == newValue) {
			throw new IllegalArgumentException("cannot accept missingValue");
		}
		
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		int existingValue;
		while (missingValue != (existingValue = values[index])) {
			if (Objects.equals(keys[index], key)) {
				if (oldValue == existingValue) {
					values[index] = newValue;
					return true;
				}
				
				break;
			}
			
			index = ++index & mask;
		}
		
		return false;
	}
	
	public void replaceAllInt(final ObjectIntToIntFunction<? super K> function) {
		requireNonNull(function);
		final int missingValue = this.missingValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int length = values.length;
		
		for (int index = 0, remaining = size; remaining > 0 && index < length; index++) {
			final int oldValue = values[index];
			if (missingValue != oldValue) {
				final int newVal = function.apply(keys[index], oldValue);
				if (missingValue == newVal) {
					throw new IllegalArgumentException("cannot except missingValue");
				}
				values[index] = newVal;
				--remaining;
			}
		}
	}
	
	public void forEach(final BiConsumer<? super K, ? super Integer> action) {
		forEachInt(action::accept);
	}
	
	public void forEachInt(final ObjIntConsumer<? super K> action) {
		requireNonNull(action);
		final int missingValue = this.missingValue;
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int length = values.length;
		
		for (int index = 0, remaining = size; remaining > 0 && index < length; index++) {
			final int oldValue = values[index];
			if (missingValue != oldValue) {
				action.accept(keys[index], oldValue);
				--remaining;
			}
		}
	}
	
	public void increaseCapacity() {
		if (size > resizeThreshold) {
			final int newCapacity = values.length << 1;
			if (newCapacity < 0) {
				throw new IllegalStateException("max capacity reached at size=" + size);
			}
			
			rehash(newCapacity);
		}
	}
	
	public void rehash(final int newCapacity) {
		final int mask = newCapacity - 1;
		resizeThreshold = (int) (newCapacity * loadFactor);
		
		@SuppressWarnings("unchecked")
		final K[] tempKeys = (K[]) new Object[newCapacity];
		final int[] tempValues = new int[newCapacity];
		Arrays.fill(tempValues, missingValue);
	
		final K[] keys = this.keys;
		final int[] values = this.values;
		
		for (int i = 0, size = values.length; i < size; i++) {
			final int value = values[i];
			if (missingValue != value) {
				final K key = keys[i];
				int index = Hashing.hash(key, mask);
				while (missingValue != tempValues[index]) {
					index = ++index & mask;
				}
				
				tempKeys[index] = key;
				tempValues[index] = value;
			}
		}
		
		this.keys = tempKeys;
		this.values = tempValues;
	}
	
	@SuppressWarnings("FinalParameters")
	private void compactChain(int deleteIndex) {
		final K[] keys = this.keys;
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = deleteIndex;
		
		while (true) {
			index = ++index & mask;
			final int value = values[index];
			if (missingValue == value) {
				break;
			}
			
			final K key = keys[index];
			final int hash = Hashing.hash(key, mask);
			
			if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) || 
					(hash <= deleteIndex && deleteIndex <= index)) {
				keys[deleteIndex] = key;
				values[deleteIndex] = value;
				
				keys[index] = null;
				values[index] = missingValue;
				deleteIndex = index;
			}
		}
	}
	
	private Integer valueOrNull(final int value) {
		return value == missingValue ? null : value;
	}
	
	public final class KeySet extends AbstractSet<K> {
		private final KeyIterator keyIterator = shouldAvoidAllocation ? new KeyIterator() : null;
		
		public KeySet() {
			
		}
		
		public KeyIterator iterator() {
			KeyIterator keyIterator = this.keyIterator;
			if (keyIterator == null) {
				keyIterator = new KeyIterator();
			}
			
			keyIterator.reset();
			return keyIterator;
		}
		
		public int size() {
			return Object2IntHashMap.this.size();
		}
		
		public boolean contains(final Object o) {
			return Object2IntHashMap.this.containsKey(o);
		}
		
		@SuppressWarnings("unchecked")
		public boolean remove(final Object o) {
			return missingValue != Object2IntHashMap.this.removeKey((K) o);
		}
		
		public void clear() {
			Object2IntHashMap.this.clear();
		}
	}
	
	public final class ValueCollection extends AbstractCollection<Integer> {
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
			return Object2IntHashMap.this.size();
		}
		
		public boolean contains(final Object o) {
			return containsValue(o);
		}
		
		public boolean contains(final int value) {
			return containsValue(value);
		}
		
		public void clear() {
			Object2IntHashMap.this.clear();
		}
		
		public boolean removeIfInt(final IntPredicate filter) {
			boolean removed = false;
			final ValueIterator iterator = iterator();
			while (iterator.hasNext()) {
				if (filter.test(iterator.nextInt())) {
					iterator.remove();
					removed = true;
				}
			}
			
			return removed;
		}
	}
	
	public final class EntrySet extends AbstractSet<Map.Entry<K, Integer>> {
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
			return Object2IntHashMap.this.size();
		}
		
		public void clear() {
			Object2IntHashMap.this.clear();
		}
		
		public boolean contains(final Object o) {
			if (!(o instanceof Entry)) {
				return false;
			}
			
			@SuppressWarnings("rawtypes")
			final Entry entry = (Entry)o;
			final Integer value = get(entry.getKey());
			
			return value != null && value.equals(entry.getValue());
		}
		
		public boolean removeIfInt(final ObjIntPredicate<? super K> filter) {
			boolean removed = false;
			final EntryIterator iterator = iterator();
			while (iterator.hasNext()) {
				iterator.findNext();
				if (filter.test(iterator.getKey(), iterator.getIntValue())) {
					iterator.remove();
					removed = true;
				}
			}
			
			return removed;
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
					array[i] = (T) it.allocateDuplicateEntry();
				} else {
					array[i] = null;
					break;
				}
			}
			
			return array;
		}
	}
	
	abstract class AbstractIterator<T> implements Iterator<T> {
		private int posCounter;
		private int stopCounter;
		private int remaining;
		private boolean isPositionValid = false;
	
		protected final int position() {
			return posCounter & (values.length - 1);
		}
		
		public boolean hasNext() {
			return remaining > 0;
		}
		
		protected final void findNext() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			
			final int missingValue = Object2IntHashMap.this.missingValue;
			final int[] values = Object2IntHashMap.this.values;
			final int mask = values.length - 1;
			
			for (int i = posCounter - 1; i >= stopCounter; i--) {
				final int index = i & mask;
				if (missingValue != values[index]) {
					posCounter = i;
					isPositionValid = true;
					--remaining;
					
					return;
				}
			}
			
			isPositionValid = false;
			throw new IllegalStateException();
		}
		
		public abstract T next();
		
		public void remove() {
			if (isPositionValid) {
				final int position = position();
				values[position] = missingValue;
				keys[position] = null;
				--size;
				
				compactChain(position);
				
				isPositionValid = false;
			} else {
				throw new IllegalStateException();
			}
		}
		
		final void reset() {
			remaining = Object2IntHashMap.this.size;
			final int[] values = Object2IntHashMap.this.values;
			final int capacity = values.length;
			
			int i = capacity;
			if (missingValue != values[capacity - 1]) {
				for (i = 0; i < capacity; i++) {
					if (missingValue == values[i]) {
						break;
					}
				}
			}
			
			stopCounter = i;
			posCounter = i + capacity;
			isPositionValid = false;
		}
	}
	
	public final class ValueIterator extends AbstractIterator<Integer> {
		public ValueIterator() {
			
		}
		
		public Integer next() {
			return nextInt();
		}
		
		public int nextInt() {
			findNext();
			return values[position()];
		}
	}
	
	public final class KeyIterator extends AbstractIterator<K> {
		public KeyIterator() {
			
		}
		
		public K next() {
			findNext();
			return keys[position()];
		}
	}
	
	public final class EntryIterator 
		extends AbstractIterator<Entry<K, Integer>> 
		implements Entry<K, Integer> {
		
		public EntryIterator() {
			
		}
		
		public Entry<K, Integer> next() {
			findNext();
			if (shouldAvoidAllocation) {
				return this;
			}
			
			return allocateDuplicateEntry();
		}
		
		private Entry<K, Integer> allocateDuplicateEntry() {
			return new MapEntry(getKey(), getIntValue());
		}
		
		public K getKey() {
			return keys[position()];
		}
		
		public int getIntValue() {
			return values[position()];
		}
		
		public Integer getValue() {
			return getIntValue();
		}
		
		public Integer setValue(final Integer value) {
			return setValue((int) value);
		}
		
		public int setValue(final int value) {
			if (missingValue == value) {
				throw new IllegalArgumentException("cannot accept missingValue");
			}
			
			final int pos = position();
			final int oldValue = values[pos];
			values[pos] = value;
			
			return oldValue;
		}
		
		public int hashCode() {
			return getKey().hashCode() ^ Integer.hashCode(getIntValue());
		}
		
		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			
			if (!(o instanceof Entry)) {
				return false;
			}
			
			final Entry<?, ?> e = (Entry<?, ?>) o;
			return Objects.equals(getKey(), e.getKey()) && 
					e.getValue() instanceof Integer && 
					getIntValue() == (Integer)e.getValue();
		}
		
		public final class MapEntry implements Entry<K, Integer> {
			private final K k;
			private int v;
			
			public MapEntry(final K k, final int v) {
				this.k = k;
				this.v = v;
			}
			
			public K getKey() {
				return k;
			}
			
			public Integer getValue() {
				return v;
			}
			
			public Integer setValue(final Integer value) {
				final Integer oldValue = Object2IntHashMap.this.put(k, value);
				v = value;
				return oldValue;
			}
			
			public int hashCode() {
				return getKey().hashCode() ^ Integer.hashCode(v);
			}
			
			public boolean equals(final Object o) {
				if (this == o) {
					return true;
				}
				
				if (!(o instanceof Entry)) {
					return false;
				}
				
				final Entry<?, ?> e = (Entry<?, ?>)o;
				
				return Objects.equals(getKey(), e.getKey()) &&
					e.getValue() instanceof Integer &&
					v == (Integer) e.getValue();
 			}
			
			public String toString() {
				return k + "=" + v;
			}
		}
	}
}
