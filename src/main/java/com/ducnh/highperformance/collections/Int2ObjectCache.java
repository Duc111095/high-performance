package com.ducnh.highperformance.collections;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import static com.ducnh.highperformance.collections.CollectionUtil.validatePositivePowerOfTwo;
import static java.util.Objects.requireNonNull;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Iterator;

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
	
	public V computeIfPresent(final int key, final IntObjectToObjectFunction<? super V, ? extends V> remappingFunction) {
		requireNonNull(remappingFunction);
		final V oldValue = get(key);
		if (null != oldValue) {
			final V newValue = remappingFunction.apply(key, oldValue);
			if (null != newValue) {
				put(key, newValue);
				return newValue;
			}
			else {
				remove(key);
				return null;
			}
		}
		return null;
	}
	
	
	public V compute(final Integer key, final BiFunction<? super Integer, ? super V, ? extends V> remappingFunction) {
		return compute((int) key, remappingFunction::apply);
	}
	
	public V compute(final int key, final IntObjectToObjectFunction<? super V, ? extends V> remappingFunction) {
		final V oldValue = get(key);
		final V newValue = remappingFunction.apply(key, oldValue);
		if (null != newValue) {
			put(key, newValue);
			return newValue;
		} else {
			if (null != oldValue) {
				remove(key);
			}
			return null;
		}
	}
	
	public V merge(final Integer key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		return merge((int) key, value, remappingFunction);
	}
	
	public V merge(final int key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		requireNonNull(value);
		requireNonNull(remappingFunction);
		final V oldValue = get(key);
		final V newValue = null == oldValue ? value : remappingFunction.apply(oldValue, value);
		if (null != newValue) {
			put(key, newValue);
			return newValue;
		} else {
			remove(key);
			return null;
		}
	}
	
	public V putIfAbsent(final Integer key, final V value) {
		return putIfAbsent((int)key, value);
	}
	
	public V putIfAbsent(final int key, final V value) {
		final V existingValue = get(key);
		if (null == existingValue) {
			put(key, value);
			return null;
		}
		
		return existingValue;
	}
	
	public V put(final Integer key, final V value) {
		return put((int) key, value);
	}
	
	@SuppressWarnings("unchecked")
	public V put(final int key, final V value) {
		requireNonNull(value, "null values are not supported");
		
		final int setNumber = Hashing.hash(key, mask);
		final int setBeginIndex = setNumber << setSizeShift;
		int i = setBeginIndex;
		
		final Object[] values = this.values;
		final int[] keys = this.keys;
		Object evictedValue = null;
		for (int nextSetIndex = setBeginIndex + setSize; i < nextSetIndex; i++) {
			evictedValue = values[i];
			if (null == evictedValue) {
				break;
			}
			
			if (key == keys[i]) {
				shuffleUp(i, nextSetIndex - 1);
				
				break;
			}
		}
		
		if (null == evictedValue) {
			evictedValue = values[setBeginIndex + (setSize - 1)];
		}
		
		shuffleDown(setBeginIndex);
		
		keys[setBeginIndex] = key;
		values[setBeginIndex] = value;
		
		cachePuts++;
		
		if (null != evictedValue) {
			evictionConsumer.accept((V) evictedValue);
		} else {
			++size;
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public boolean remove(final Object key, final Object value) {
		return remove((int) key, (V) value);
	}
	
	public boolean remove(final int key, final V value) {
		final V existingValue = get(key);
		if (null != existingValue && Objects.equals(value, existingValue)) {
			remove(key);
			return true;
		}
		
		return false;
	}
	
	public V remove(final Object key) {
		return remove((int) key);
	}
	
	@SuppressWarnings("unchecked")
	public V remove(final int key) {
		final int setNumber = Hashing.hash(key, mask);
		final int setBeginIndex = setNumber << setSizeShift;
		
		final int[] keys = this.keys;
		final Object[] values = this.values;
		Object value = null;
		for (int i = setBeginIndex, nextSetIndex = setBeginIndex + setSize; i < nextSetIndex; i++) {
			value = values[i];
			if (null == value) {
				break;
			} 
			
			if (key == keys[i]) {
				shuffle(i, nextSetIndex - 1);
				--size;
				
				evictionConsumer.accept((V) value);
				break;
			}
		}
		
		return (V) value;
	}
	
	public boolean replace(final Integer key, final V oldValue, final V value) {
		return replace((int) key, oldValue, value);
	}
	
	public boolean replace(final int key, final V oldValue, final V newValue) {
		final V existingValue = get(key);
		if (null != existingValue && Objects.equals(existingValue, oldValue)) {
			put(key, newValue);
			return true;
		}
		
		return false;
	}
	
	public V replace(final Integer key, final V value) {
		return replace((int) key, value);
	}
	
	public V replace(final int key, final V value) {
		final V oldValue = get(key);
		if (null != oldValue) {
			put(key, value);
		}
		return oldValue;
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
				final V newValue = function.apply(keys[index], (V)oldValue);
				requireNonNull(newValue, "null values are not supported");
				values[index] = newValue;
				--remaining;
			}
		} 
	}
	
	private void shuffleUp(final int fromIndex, final int toIndex) {
		final int[] keys = this.keys;
		final Object[] values = this.values;
		
		for (int i = fromIndex; i < toIndex; i++) {
			values[i] = values[i + 1];
			keys[i] = keys[i + 1];
		}
		
		values[toIndex] = null;
	}
	
	private void shuffleDown(final int setBeginIndex) {
		final int[] keys = this.keys;
		final Object[] values = this.values;
		for (int i = setBeginIndex + (setSize - 1); i > setBeginIndex; i--) {
			values[i] = values[i - 1];
			keys[i] = keys[i - 1];
		}
		values[setBeginIndex] = null;
	}
	
	@SuppressWarnings("unchecked")
	public void clear() {
		final Object[] values = this.values;
		for (int i = 0, length = values.length; i < length; i++) {
			final Object value = values[i];
			if (null != value) {
				values[i] = null;
				size--;
				
				evictionConsumer.accept((V)value);
			}
		}
	}
	
	public void putAll(final Map<? extends Integer, ? extends V> map) {
		for (final Entry<? extends Integer, ? extends V> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}
	
	public void putAll(final Int2ObjectCache<? extends V> map) {
		final Int2ObjectCache<? extends V>.EntryIterator iterator = map.entrySet().iterator();
		
		while (iterator.hasNext()) {
			iterator.findNext();
			put(iterator.getIntKey(), iterator.getValue());
		}
	}
	
	public KeySet keySet() {
		return keySet == null ? new KeySet() : keySet;
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
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append('{');
		
		final int[] keys = this.keys;
		final Object[] values = this.values;
		for (int i = 0, length = values.length; i < length; i++) {
			final Object value = values[i];
			if (null != value) {
				sb.append(keys[i]).append('=').append(value).append(", ");
			}
		}
		
		if (sb.length() > 1) {
			sb.setLength(sb.length() - 1);
		}
		
		sb.append('}');
		return sb.toString();
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
				if (!thisValue.equals(thatValue)) {
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
			if (null != value) {
				result += (Integer.hashCode(keys[i]) ^ value.hashCode());
			}
		}
		
		return result;
	}
	
	public final class KeySet extends AbstractSet<Integer> {
		private final KeyIterator iterator = new KeyIterator();
		
		public KeySet() {
			
		}
		
		public int size() {
			return Int2ObjectCache.this.size();
		}
		
		public boolean contains(final Object o) {
			return Int2ObjectCache.this.containsKey(o); 
		}
		
		public boolean contains(final int key) {
			return Int2ObjectCache.this.containsKey(key);
		}
		
		public KeyIterator iterator() {
			iterator.reset();
			
			return iterator;
		}
		
		public boolean remove(final Object o) {
			throw new UnsupportedOperationException("Cannot remove from KeySet");
		}
		
		public boolean removeIf(final Predicate<? super Integer> filter) {
			throw new UnsupportedOperationException("Cannot remove from KeySet");
		}
		
		public void clear() {
			Int2ObjectCache.this.clear();
		}
	}
	
	public final class ValueCollection extends AbstractCollection<V> {
		private final ValueIterator iterator = new ValueIterator();
		
		public ValueCollection() {
			
		}
		
		public int size() {
			return Int2ObjectCache.this.size();
		}
		
		public boolean contains(final Object o) {
			return Int2ObjectCache.this.containsValue(o);
		}
		
		public ValueIterator iterator() {
			return iterator.reset();
		}
		
		public void clear() {
			Int2ObjectClear.this.clear();
		}
		
		public boolean remove(final Object o) {
			throw new UnsupportedOperationException("Cannot remove from ValueCollection");
		}
		
		public boolean removeIf(final Predicate<? super V> filter) {
			throw new UnsupportedOperationException("Cannot remove from ValueCollection");
		}
	}
	
	public final class EntrySet extends AbstractSet<Map.Entry<Integer, V>> {
		private final EntryIterator iterator = new EntryIterator();
		
		public EntrySet() {
			
		}
		
		public int size() {
			return Int2ObjectCache.this.size();
		}
		
		public EntryIterator iterator() {
			iterator.reset();
			
			return iterator;
		}
		
		public void clear() {
			Int2ObjectCache.this.clear();
		}
		
		public boolean remove(final Object o) {
			throw new UnsupportedOperationException("Cannot remove from EntrySet");
		}
		
		public boolean removeIf(final Predicate<? super Entry<Integer, V>> filter) {
			throw new UnsupportedOperationException("Cannot remove from EntrySet");
		}
	}
	
	abstract class AbstractIterator<T> implements Iterator<T> {
		private int remaining;
		private int position = -1;
	
		protected final int position() {
			return position;
		}
		
		public boolean hasNext() {
			return remaining > 0;
		}
		
		protected final void findNext() {
			boolean found = false;
			final Object[] values = Int2ObjectCache.this.values;
			
			for (int i = position + 1, size = capacity; i < size; i++) {
				if (null != values[i]) {
					found = true;
					position = 1;
					--remaining;
					break;
				} 
			}
			if (!found) {
				throw new NoSuchElementException();
			}
		}
		
		public abstract T next();
		
		public void remove() {
			throw new UnsupportedOperationException("Remove not supported on Iterator");
		}
		
		void reset() {
			remaining = size;
			position = -1;
		}
	}
	
	public final class ValueIterator extends AbstractIterator<V> {
		public ValueIterator() {
			
		}
		
		@SuppressWarnings("unchecked")
		public V next() {
			findNext();
			return (V)values[position()];
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
	
	public final class EntryIterator extends AbstractIterator<Entry<Integer, V>> 
		implements Entry<Integer, V>{
		
		public EntryIterator() {
			
		}
		
		public Entry<Integer, V> next() {
			findNext();
			
			return this;
		}
		
		public Integer getKey() {
			return getIntKey();
		}
		
		public int getIntKey() {
			return keys[position()];
		}
		
		@SuppressWarnings("unchecked")
		public V getValue() {
			return (V) values[position()];
		}
		
		public V setValue(final V value) {
			throw new UnsupportedOperationException("no set on this iterator");
		}
	}
}
