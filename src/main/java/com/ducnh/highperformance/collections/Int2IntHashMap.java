package com.ducnh.highperformance.collections;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

import static java.util.Objects.requireNonNull;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;

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
	
	public Integer replace(final Integer key, final Integer value) {
		return valOrNull(replace((int)key, (int)value));
	}
	
	public boolean replace(final Integer key, final Integer oldValue, final Integer newValue) {
		return replace((int)key, (int)oldValue, (int)newValue);
	}
	
	public void replaceAll(final BiFunction<? super Integer, ? super Integer, ? super Integer> function) {
		replaceAllInt(function::apply);
	}
	
	public KeySet keySet() {
		if (keySet == null) {
			keySet = new KeySet();
		}
		return keySet;
	}
	
	public ValueCollection values() {
		if (values == null) {
			values = new ValueCollection();
		}
		return values;
	}
	
	public EntrySet entrySet() {
		if (entrySet == null) {
			entrySet = new EntrySet();
		}
		return entrySet;
	}
	
	public Integer remove(final Object key) {
		return valOrNull(remove((int)key));
	}
	
	public boolean remove(final Object key, final Object value) {
		return remove((int)key, (int)value);
	}
	
	public int remove(final int key) {
		final int missingValue = this.missingValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = Hashing.evenHash(key, mask);
	
		int oldValue;
		while (missingValue != (oldValue = entries[keyIndex + 1])) {
			if (key == entries[keyIndex]) {
				entries[keyIndex + 1] = missingValue;
				size--;
				
				compactChain(keyIndex);
				break;
			}
			
			keyIndex = next(keyIndex, mask);
		}
		
		return oldValue;
	}
	
	public boolean remove(final int key, final int value) {
		final int missingValue = this.missingValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = Hashing.evenHash(key, mask);
		
		int oldValue;
		while (missingValue != (oldValue = entries[keyIndex + 1])) {
			if (key == entries[keyIndex]) {
				if (value == oldValue) {
					entries[keyIndex + 1] = missingValue;
					size--;
					
					compactChain(keyIndex);
					return true;
				}
				break;
			}
			keyIndex = next(keyIndex, mask);
		}
		
		return false;
	}
	
	public int merge(final int key, final int value, final IntIntFunction remappingFunction) {
		requireNonNull(remappingFunction);
		final int missingValue = this.missingValue;
		if (value == missingValue) {
			throw new IllegalArgumentException("cannot accept missingValue");
		}
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
		
		final int newValue = missingValue == oldValue ? value : remappingFunction.apply(oldValue, value);
		if (missingValue != newValue) {
			entries[index + 1] = newValue;
			if (missingValue == oldValue) {
				entries[index] = key;
				++size;
				increaseCapacity();
			}
		} else {
			entries[index + 1] = missingValue;
			size--;
			compactChain(index);
		}
		
		return newValue;
	}
	
	@SuppressWarnings("FinalParameters")
	private void compactChain(int deleteKeyIndex) {
		final int missingValue = this.missingValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = deleteKeyIndex;
		
		while (true) {
			keyIndex = next(keyIndex, mask);
			final int value = entries[keyIndex + 1];
			if (value == missingValue) {
				break;
			}
			
			final int key = entries[keyIndex];
			final int hash = Hashing.evenHash(key, mask);
			if ((keyIndex < hash && (hash <= deleteKeyIndex || deleteKeyIndex <= keyIndex)) ||
					(hash <= deleteKeyIndex && deleteKeyIndex <= keyIndex)) {
				entries[deleteKeyIndex] = key;
				entries[deleteKeyIndex + 1] = value;
				
				entries[keyIndex + 1] = missingValue;
				deleteKeyIndex = keyIndex;
			}
		}
	}
	
	public int minValue() {
		final int missingValue = this.missingValue;
		int min = 0 == size ? missingValue : Integer.MAX_VALUE;
		final int[] entries = this.entries;
		final int length = entries.length;
		for (int valueIndex = 1; valueIndex < length; valueIndex += 2) {
			final int value = entries[valueIndex];
			if (missingValue != value) {
				min = Math.min(min, value);
			} 
		}
		
		return min;
	}
	
	public int maxValue() {
		final int missingValue = this.missingValue;
		int max = 0 == size ? missingValue : Integer.MIN_VALUE;
		final int[] entries = this.entries;
		final int length = entries.length;
		for (int valueIndex = 1; valueIndex < length; valueIndex += 2) {
			final int value = entries[valueIndex];
			if (missingValue != value) {
				max = Math.max(max, value);
			}
		}
		
		return max;
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
			sb.append(entryIterator.getIntKey()).append('=').append(entryIterator.getIntValue());
			if (!entryIterator.hasNext()) {
				return sb.append('}').toString();
			}
			sb.append(',').append(' ');
		}
	}
	
	public int replace(final int key, final int value) {
		final int missingValue = this.missingValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = Hashing.evenHash(key, mask);
		
		int oldValue;
		while (missingValue != (oldValue = entries[keyIndex + 1])) {
			if (key == entries[keyIndex]) {
				entries[keyIndex + 1] = value;
				break;
			}
			
			keyIndex = next(keyIndex, mask);
		}
		
		return oldValue;
	}
	
	public boolean replace(final int key, final int oldValue, final int newValue) {
		final int missingValue = this.missingValue;
		final int[] entries = this.entries;
		final int mask = entries.length - 1;
		int keyIndex = Hashing.evenHash(key, mask);
		
		int value;
		while (missingValue != (value = entries[keyIndex + 1])) {
			if (key == entries[keyIndex]) {
				if (oldValue == value) {
					entries[keyIndex + 1] = newValue;
					return true;
				}
				break;
			}
			
			keyIndex = next(keyIndex, mask);
		}
		return false;
	}
	
	public void replaceAllInt(final IntIntFunction function) {
		requireNonNull(function);
		final int missingValue = this.missingValue;
		final int[] entries = this.entries;
		final int length = entries.length;
		
		for (int valueIndex = 1, remaining = size; remaining > 0 && valueIndex < length; valueIndex += 2) {
			final int existingValue = entries[valueIndex];
			if (missingValue != existingValue) {
				final int newValue = function.apply(entries[valueIndex - 1], existingValue);
				if (missingValue == newValue) {
					throw new IllegalArgumentException("cannot replace with a missingValue");
				}
				entries[valueIndex] = newValue;
				--remaining;
			}
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
		return size == that.size() && entrySet().equals(that.entrySet());
	}
	
	public int hashCode() {
		return entrySet().hashCode();
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
		entries = new int[entriesLength];
		Arrays.fill(entries, missingValue);
	}
	
	private Integer valOrNull(final int value) {
		return value == missingValue ? null : value;
	}
	
	abstract class AbstractIterator {
		protected boolean isPositionValid = false;
		private int remaining;
		private int positionCounter;
		private int stopCounter;
		
		final void reset() {
			isPositionValid = false;
			remaining = Int2IntHashMap.this.size;
			final int missingValue = Int2IntHashMap.this.missingValue;
			final int[] entries = Int2IntHashMap.this.entries;
			final int capacity = entries.length;
			
			int keyIndex = capacity;
			if (missingValue != entries[capacity - 1]) {
				for (int i = 1; i < capacity; i += 2) {
					if (missingValue == entries[i]) {
						keyIndex = i - 1;
						break;
					}
				}
			}
			
			stopCounter = keyIndex;
			positionCounter = keyIndex + capacity;
		}
		
		protected final int keyPosition() {
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
			
			final int[] entries = Int2IntHashMap.this.entries;
			final int missingValue = Int2IntHashMap.this.missingValue;
			final int mask = entries.length - 1;
			
			for (int keyIndex = positionCounter - 2, stop = stopCounter; keyIndex >= stop; keyIndex -= 2) {
				final int index = keyIndex & mask;
				if (missingValue != entries[index + 1]) {
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
				entries[position + 1] = missingValue;
				--size;
				
				compactChain(position);
				isPositionValid = false;
			}
			else {
				throw new IllegalStateException();
			}
		}
	}
	
	public final class KeyIterator extends AbstractIterator implements Iterator<Integer> {
		public KeyIterator() {
			
		}
		
		public Integer next() {
			return nextValue();
		}
		
		public int nextValue() {
			findNext();
			return entries[keyPosition()];
		}
	}
	
	public final class ValueIterator extends AbstractIterator implements Iterator<Integer> {
		public ValueIterator() {
			
		}
		
		public Integer next() {
			return nextValue();
		}

		public int nextValue() {
			findNext();
			return entries[keyPosition() + 1];
		}
	}
	
	public final class EntryIterator extends AbstractIterator implements Iterator<Entry<Integer, Integer>>, Entry<Integer, Integer> {
		public EntryIterator() {
			
		}
		
		public Integer getKey() {
			return getIntKey();
		}
		
		public int getIntKey() {
			return entries[keyPosition()];
		}
		
		public Integer getValue() {
			return getIntValue();
		}
		
		public int getIntValue() {
			return entries[keyPosition() + 1];
		}
		
		public Integer setValue(final Integer value) {
			return setValue(value.intValue());
		}
		
		public int setValue(final int value) {
			if (!isPositionValid) {
				throw new IllegalStateException();
			}
			
			if (missingValue == value) {
				throw new IllegalArgumentException("cannot except missingValue");
			}
			
			final int keyPosition = keyPosition();
			final int[] entries = Int2IntHashMap.this.entries;
			final int preValue = entries[keyPosition  +1];
			entries[keyPosition + 1] = value;
			return preValue;
		}
		
		public Entry<Integer, Integer> next() {
			findNext();
			if (shouldAvoidAllocation) {
				return this;
			}
			return allocateDuplicateEntry();
		}
		
		private Entry<Integer, Integer> allocateDuplicateEntry() {
			return new MapEntry(getIntKey(), getIntValue());
		}
		
		public int hashCode() {
			return Integer.hashCode(getIntKey()) ^ Integer.hashCode(getIntValue());
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
		
		public final class MapEntry implements Entry<Integer, Integer> {
			private final int k;
			private final int v;
			
			public MapEntry(final int k, final int v) {
				this.k = k;
				this.v = v;
			}
			
			public Integer getKey() {
				return k;
			}
			
			public Integer getValue() {
				return v;
			}
			
			public Integer setValue(final Integer value) {
				return Integer.hashCode(getIntKey()) ^ Integer.hashCode(getIntValue());
			}
			
			public boolean equals(final Object o) {
				if (!(o instanceof Map.Entry)) {
					return false;
				}
				
				final Entry<?, ?> e = (Entry<?, ?>)o;
				
				return (e.getKey() != null && e.getValue() != null && (e.getKey().equals(k) && e.getValue().equals(v)));
			}
			
			public String toString() {
				return k + "=" + v;
			}
		}
	}
	
	public final class KeySet extends AbstractSet<Integer> {
		private final KeyIterator keyIterator = shouldAvoidAllocation ? new KeyIterator() : null;
		public KeySet() {}
		
		public KeyIterator iterator() {
			KeyIterator keyIterator = this.keyIterator;
			if (null == keyIterator) {
				keyIterator = new KeyIterator();
			}
			
			keyIterator.reset();
			return keyIterator;
		}
		
		public int size() {
			return Int2IntHashMap.this.size();
		}
		
		public boolean isEmpty() {
			return Int2IntHashMap.this.isEmpty();
		}
		
		public void clear() {
			Int2IntHashMap.this.clear();
		}
		
		public boolean contains(final Object o) {
			return contains((int)o);
		}
		
		public boolean contains(final int key) {
			return containsKey(key);
		}
		
		public boolean removeIfInt(final IntPredicate filter) {
			boolean removed = false;
			final KeyIterator iterator = iterator();
			while (iterator.hasNext()) {
				if (filter.test(iterator.nextValue())) {
					iterator.remove();
					removed = true;
				}
			}
			return removed;
		}
	} 
	
	public final class ValueCollection extends AbstractCollection<Integer> {
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
			return Int2IntHashMap.this.size();
		}
		
		public boolean contains(final Object o) {
			return contains((int)o);
		}
		
		public boolean contains(final int value) {
			return containsValue(value);
		}
		
		public boolean removeIfInt(final IntPredicate filter) {
			boolean removed = false;
			final ValueIterator iterator = iterator();
			while (iterator.hasNext()) {
				if (filter.test(iterator.nextValue())) {
					iterator.remove();
					removed = true;
				}
			}
			return removed;
		}
	}
	
	public final class EntrySet extends AbstractSet<Map.Entry<Integer, Integer>> {
		private final EntryIterator entryIterator = shouldAvoidAllocation ? new EntryIterator() : null;
		
		public EntrySet() {}
		
		public EntryIterator iterator() {
			EntryIterator entryIterator = this.entryIterator;
			if (entryIterator == null) {
				entryIterator = new EntryIterator();
			}
			
			entryIterator.reset();
			
			return entryIterator;
		}
		
		public int size() {
			return Int2IntHashMap.this.size();
		}
		
		public boolean isEmpty() {
			return Int2IntHashMap.this.isEmpty();
		}
		
		public void clear() {
			Int2IntHashMap.this.clear();
		}
		
		public boolean contains(final Object o) {
			if (!(o instanceof Entry)) {
				return false;
			}
			
			final Entry<?, ?> entry = (Entry<?, ?>)o;
			final Integer value = get(entry.getKey());
			
			return value != null && value.equals(entry.getValue());
		}
		
		public boolean removeIfInt(final IntIntPredicate filter) {
			boolean removed = false;
			final EntryIterator iterator = iterator();
			while (iterator.hasNext()) {
				iterator.findNext();
				if (filter.test(iterator.getIntKey(), iterator.getIntValue())) {
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
					a : (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
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
