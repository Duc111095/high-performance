package com.ducnh.highperformance.collections;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import com.ducnh.highperformance.collections.Int2IntHashMap.EntryIterator;

import static com.ducnh.highperformance.collections.CollectionUtil.validateLoadFactor;
import static com.ducnh.highperformance.BitUtil.findNextPositivePowerOfTwo;
import static java.util.Objects.requireNonNull;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;

public class Int2ObjectHashMap<V> implements Map<Integer, V>{
	static final int MIN_CAPACITY = 8;
	private final float loadFactor;
	private int resizeThreshold;
	private int size;
	private final boolean shouldAvoidAllocation;
	
	private int[] keys;
	private Object[] values;
	
	private ValueCollection valueCollection;
	private KeySet keySet;
	private EntrySet entrySet;
	
	public Int2ObjectHashMap() {
		this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, true);
	}
	
	public Int2ObjectHashMap(
			final int initialCapacity,
			final float loadFactor,
			final boolean shouldAvoidAllocation) {
		validateLoadFactor(loadFactor);
		
		this.loadFactor = loadFactor;
		this.shouldAvoidAllocation = shouldAvoidAllocation;
		final int capacity = findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity));
		resizeThreshold = (int)(capacity * loadFactor);
		
		keys = new int[capacity];
		values = new Object[capacity];
	}
	
	public Int2ObjectHashMap(final Int2ObjectHashMap<V> mapToCopy) {
		this.loadFactor = mapToCopy.loadFactor;
		this.resizeThreshold = mapToCopy.resizeThreshold;
		this.size = mapToCopy.size;
		this.shouldAvoidAllocation = mapToCopy.shouldAvoidAllocation;
		
		keys = mapToCopy.keys.clone();
		values = mapToCopy.values.clone();
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
		return size == 0;
	}
	
	public void forEach(final BiConsumer<? super Integer, ? super V> action) {
		forEachInt(action::accept);
	}
	
	public void forEachInt(final IntObjConsumer<V> consumer) {
		requireNonNull(consumer);
		final int[] keys = this.keys;
		final Object[] values = this.values;
		final int length = values.length;
		
		for (int index = 0, remaining = size; remaining > 0 && index < length; index++) {
			final Object value = values[index];
			if (value != null) {
				consumer.accept(keys[index], unmapNullValue(value));
				--remaining;
			}
		}
	}
	
	public boolean containsKey(final Object key) {
		return containsKey((int) key);
	}
	
	public boolean containsKey(final int key) {
		final int[] keys = this.keys;
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
	
	public boolean containsValue(final Object value) {
		boolean found = false;
		final Object val = mapNullValue(value);
		
		if (null != val) {
			final Object[] values = this.values;
			final int length = values.length;
			for (int i = 0, remaining  = size; remaining > 0  && i < length; i++) {
				final Object existingValue = values[i];
				if (existingValue != null) {
					if (Objects.equals(existingValue, val)) {
						found = true;
						break;
					}
					--remaining;
				}
			}
		}
		return found;
	}
	
	public V get(final Object key) {
		return get((int)key);
	}
	
	public V get(final int key) {
		return unmapNullValue(getMapped(key));
	}
	
	public V getOrDefault(final int key, final V defaultValue) {
		final V value = getMapped(key);
		return null != value ? unmapNullValue(value) : defaultValue;
	}
	
	@SuppressWarnings("unchecked")
	protected V getMapped(final int key) {
		final int[] keys = this.keys;
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
		
		return (V)value;
	}
	
	public V computeIfAbsent(final Integer key, final Function<? super Integer, ? extends V> mappingFunction) {
		return computeIfAbsent((int)key, mappingFunction::apply);
	}
	
	public V computeIfAbsent(final int key, final IntFunction<? extends V> mappingFunction) {
		requireNonNull(mappingFunction);
		final int[] keys = this.keys;
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
		
		if (null == value && (value = mappingFunction.apply(key)) != null) {
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
			final Integer key, final BiFunction<? super Integer, ? super V, ? extends V> remappingFunction) {
		return computeIfPresent((int)key, remappingFunction::apply);
	}
	
	public V computeIfPresent(
			final int key, final IntObjectToObjectFunction<? super V, ? extends V> remappingFunction) {
		requireNonNull(remappingFunction);
		final int[] keys = this.keys;
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
			value = remappingFunction.apply(key, value);
			values[index] = value;
			if (null == value) {
				--size;
				compactChain(index);
			}
		}
		
		return value;
	}
	
	public V compute(final Integer key, final BiFunction<? super Integer, ? super V, ? extends V> remappingFunction) {
		return compute((int) key, remappingFunction::apply);
	}
	
	public V compute(final int key, final IntObjectToObjectFunction<? super V, ? extends V> remappingFunction) {
		requireNonNull(remappingFunction);
		final int[] keys = this.keys;
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
		
		final V newValue = remappingFunction.apply(key, unmapNullValue(mappedValue));
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
			size--;
			compactChain(index);
		}
		
		return newValue;
	}
	
	public V merge(
			final Integer key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		return merge((int)key, value, remappingFunction);
	}
	
	public V merge(final int key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		requireNonNull(value);
		requireNonNull(remappingFunction);
		final int[] keys = this.keys;
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
			size--;
			compactChain(index);
		}
		
		return newValue;
	}
	
	public V put(final Integer key, final V value) {
		return put((int) key, value);
	}
	
	@SuppressWarnings("unchecked")
	public V put(final int key, final V value) {
		final V val = (V)mapNullValue(value);
		requireNonNull(val, "value cannot be null");
		
		final int[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object oldValue;
		while (null != (oldValue = values[index])) {
			if (key == keys[index]) {
				break;
			}
			
			index = ++index & mask;
		}
		
		if (oldValue == null) {
			++size;
			keys[index] = key;
		}
		
		values[index] = val;
		
		if (size > resizeThreshold) {
			increaseCapacity();
		}
		
		return unmapNullValue(oldValue);
	}
	
	public V remove(final Object key) {
		return remove((int) key);
	}
	
	public V remove(final int key) {
		final int[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(key, mask);
		
		Object value;
		
		while (null != (value = values[index])) {
			if (key == keys[index]) {
				values[index] = null;
				--size;
				
				compactChain(index);
				break;
			}
			
			index = ++index & mask;
		}
		
		return unmapNullValue(value);
	}
	
	@SuppressWarnings("unchecked")
	public boolean remove(final int key, final Object value) {
		final Object val = mapNullValue(value);
		if (val != null) {
			final int[] keys = this.keys;
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
	
	public void clear() {
		if (size > 0) {
			Arrays.fill(values, null);
			size = 0;
		}
	}
	
	public void compact() {
		final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
		rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
	}
	
	public void putAll(final Map<? extends Integer, ? extends V> map) {
		for (final Entry<? extends Integer, ? extends V> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}
	
	public void putAll(final Int2ObjectHashMap<? extends V> map) {
		final Int2ObjectHashMap<? extends V>.EntryIterator iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			iterator.findNext();
			put(iterator.getIntKey(), iterator.getValue());
		}
	}
	
	@SuppressWarnings("unchecked")
	public V putIfAbsent(final int key, final V value) {
		final V val = (V)mapNullValue(value);
		requireNonNull(val, "value cannot be null");
		
		final int[] keys = this.keys;
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
	
	public KeySet keySet() {
		if (null == keySet) {
			keySet =  new KeySet();
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
		if (entrySet == null) {
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
		while(true) {
			entryIterator.next();
			sb.append(entryIterator.getIntKey()).append('=').append(unmapNullValue(entryIterator.getValue()));
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
		
		final int[] keys = this.keys;
		final Object[] values = this.values;
		for (int i = 0, length = values.length; i < length; i++) {
			final Object thisValue = values[i];
			if (null != thisValue) {
				final Object thatValue = that.get(keys[i]);
				if (!thisValue.equals(mapNullValue(thatValue))) {
					return false;
				}
			}
		}
		return true;
	}
	
	public int hashCode() {
		int result = 0;
		final int[] keys = this.keys;
		final Object[] values = this.values;
		for (int i = 0, length = values.length; i < length; i++) {
			final Object value = values[i];
			if (value != null) {
				result += (Integer.hashCode(keys[i]) ^ value.hashCode());
			}
		}
		
		return result;
	}
	
	protected Object mapNullValue(final Object value) {
		return value;
	}
	
	@SuppressWarnings("unchecked")
	protected V unmapNullValue(final Object value) {
		return (V)value;
	}
	
	@SuppressWarnings("unchecked")
	public V replace(final int key, final V value) {
		final V val = (V)mapNullValue(value);
		requireNonNull(val, "value cannot be null");
		
		final int[] keys = this.keys;
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
	public boolean replace(final int key, final V oldValue, final V newValue) {
		final V val = (V)mapNullValue(newValue);
		requireNonNull(val, "value cannot be null");
		
		final int[] keys = this.keys;
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
			
			index  = ++index & mask;
		}
		
		return false;
	}
	
	public void replaceAll(final BiFunction<? super Integer, ? super V, ? extends V> function) {
		replaceAllInt(function::apply);
	}
	
	@SuppressWarnings("unchecked")
	public void replaceAllInt(final IntObjectToObjectFunction<? super V, ? extends V> function) {
		requireNonNull(function);
		final int[] keys = this.keys;
		final Object[] values = this.values;
		final int length = values.length;
		int remaining = size;
		
		for (int index = 0; remaining > 0 && index < length; index++) {
			final Object oldValue = values[index];
			if (null != oldValue) {
				final V newVal = (V)mapNullValue(function.apply(keys[index], unmapNullValue(oldValue)));
				requireNonNull(newVal, "value cannot be null");
				values[index] = newVal;
				--remaining;
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
	
	private void rehash(final int newCapacity) {
		final int mask = newCapacity - 1;
		resizeThreshold = (int)(newCapacity * loadFactor);
		
		final int[] tempKeys = new int[newCapacity];
		final Object[] tempValues = new Object[newCapacity];
		
		final int[] keys = this.keys;
		final Object[] values = this.values;
		for (int i = 0, size = values.length; i < size; i++) {
			final Object value = values[i];
			if (value != null) {
				final int key = keys[i];
				int index = Hashing.hash(key, mask);
				while (null != tempValues[index]) {
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
		final int[] keys = this.keys;
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = deleteIndex;
		
		while (true) {
			index = ++index & mask;
			final Object value = values[index];
			if (value == null) {
				break;
			}
			
			final int key = keys[index];
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
	
	public final class KeySet extends AbstractSet<Integer> {
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
			return Int2ObjectHashMap.this.size();
		}
		
		public boolean contains(final Object o) {
			return Int2ObjectHashMap.this.containsKey(o);
		}
		
		public boolean contains(final int key) {
			return Int2ObjectHashMap.this.containsKey(key);
		}
		
		public boolean remove(final Object o) {
			return null != Int2ObjectHashMap.this.remove(o);
		}
		
		public boolean remove(final int key) {
			return null != Int2ObjectHashMap.this.remove(key);
		}
		
		public void clear() {
			Int2ObjectHashMap.this.clear();
		}
		
		public boolean removeIfInt(final IntPredicate filter) {
			boolean removed = false;
			final KeyIterator iterator = iterator();
			while (iterator.hasNext()) {
				if (filter.test(iterator.nextInt())) {
					iterator.remove();
					removed = true;
				}
			}
			
			return removed;
		}
	}
	
	public final class ValueCollection extends AbstractCollection<V> {
		private final ValueIterator valueIterator = shouldAvoidAllocation ? new ValueIterator() : null;
		
		public ValueCollection() {
			
		}
		
		public ValueIterator iterator() {
			ValueIterator valueIterator = this.valueIterator;
			if (valueIterator == null) {
				valueIterator = new ValueIterator();
			}
			
			valueIterator.reset();
			return valueIterator;
		}
		
		public int size() {
			return Int2ObjectHashMap.this.size();
		}
		
		public boolean contains(final Object o) {
			return Int2ObjectHashMap.this.containsValue(o);
		}
		
		public void clear() {
			Int2ObjectHashMap.this.clear();
		}
		
		public void forEach(final Consumer<? super V> action) {
			int remaining = Int2ObjectHashMap.this.size;
			
			final Object[] values = Int2ObjectHashMap.this.values;
			for (int i = 0, length = values.length; remaining > 0 && i < length; i++) {
				final Object value = values[i];
				if (value != null) {
					action.accept(unmapNullValue(value));
					--remaining;
				}
			}
		}
	}
	
	public final class EntrySet extends AbstractSet<Map.Entry<Integer, V>> {
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
			return Int2ObjectHashMap.this.size();
		}
		
		public void clear() {
			Int2ObjectHashMap.this.clear();
		}
		
		public boolean contains(final Object o) {
			if (!(o instanceof Entry)) {
				return false;
			}
			
			final Entry<?, ?> entry = (Entry<?, ?>)o;
			final int key = (Integer) entry.getKey();
			final V value = getMapped(key);
			return null != value && value.equals(mapNullValue(entry.getValue()));
		}
		
		public boolean removeIfInt(final IntObjPredicate<V> filter) {
			boolean removed = false;
			final EntryIterator iterator = iterator();
			while (iterator.hasNext()) {
				iterator.findNext();
				if (filter.test(iterator.getIntKey(), iterator.getValue())) {
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
					array[i] = (T)it.allocateDuplicateEntry();
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
		boolean isPositionValid = false;
		
		protected final int position() {
			return posCounter & (values.length - 1);
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
			
			final Object[] values = Int2ObjectHashMap.this.values;
			final int mask = values.length - 1;
			
			for (int i = posCounter - 1, stop = stopCounter; i >= stop; i--) {
				final int index = i & mask;
				if (null != values[index]) {
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
				values[position] = null;
				--size;
				
				compactChain(position);
				isPositionValid = false;
			} else {
				throw new IllegalStateException();
			}
		}
		
		public void reset() {
			remaining = Int2ObjectHashMap.this.size;
			final Object[] values = Int2ObjectHashMap.this.values;
			final int capacity = values.length;
			
			int i = capacity;
			if (null != values[capacity - 1]) {
				for (i = 0; i < capacity; i++) {
					if (null == values[i]) {
						break;
					}
				}
			}
			
			stopCounter = i;
			posCounter = i + capacity;
			isPositionValid = false;
		}
	}
	
	public final class ValueIterator extends AbstractIterator<V> {
		public ValueIterator() {
			
		}
		
		public V next() {
			findNext();
			
			return unmapNullValue(values[position()]);
		}
	}
	
	public final class KeyIterator extends AbstractIterator<Integer> {
		public KeyIterator() {
			
		}
		
		public Integer next() {
			return nextInt();
		}
		
		public int nextInt() {
			findNext();
			return keys[position()];
		}
	}
	
	public final class EntryIterator extends AbstractIterator<Entry<Integer, V>> implements Entry<Integer, V>{
		public EntryIterator() {
			
		}
		
		public Entry<Integer, V> next() {
			findNext();
			if (shouldAvoidAllocation) {
				return this;
			}
			
			return allocateDuplicateEntry();
		}
		
		private Entry<Integer, V> allocateDuplicateEntry() {
			return new MapEntry(getIntKey(), getValue());
		}
		
		public Integer getKey() {
			return getIntKey();
		}
		
		public int getIntKey() {
			return keys[position()];
		}
		
		public V getValue() {
			return unmapNullValue(values[position()]);
		}
		
		@SuppressWarnings("unchecked")
		public V setValue(final V value) {
			final V val = (V)mapNullValue(value);
			requireNonNull(val, "value cannot be null");
			
			if (!this.isPositionValid) {
				throw new IllegalStateException();
			}
			
			final int pos = position();
			final Object[] values = Int2ObjectHashMap.this.values;
			final Object oldValue = values[pos];
			values[pos] = val;
			
			return (V)oldValue;
		}
		
		public final class MapEntry implements Entry<Integer, V> {
			private final int k;
			private final V v;
			
			public MapEntry(final int k , final V v) {
				this.k = k;
				this.v = v;
			}
			
			public Integer getKey() {
				return k;
			}
			
			public V getValue() {
				return v;
			}
			
			public V setValue(final V value) {
				return Int2ObjectHashMap.this.put(k, value);
			}
			
			public int hashCode() {
				return Integer.hashCode(getIntKey()) ^ (null != v ? v.hashCode() : 0);
			}
			
			public boolean equals(final Object o) {
				if (!(o instanceof Map.Entry)) {
					return false;
				}
				
				final Entry<?, ?> e = (Entry<?, ?>)o;
				
				return (e.getKey() != null && e.getKey().equals(k))
						&& ((e.getValue() == null && v == null) || e.getValue().equals(v));
			}
			
			public String toString() {
				return k + "=" + v;
			}
		}
	}
}
