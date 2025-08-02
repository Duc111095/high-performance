package com.ducnh.highperformance.collections;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static com.ducnh.highperformance.BitUtil.findNextPositivePowerOfTwo;
import static com.ducnh.highperformance.collections.CollectionUtil.validateLoadFactor;

public class ObjectHashSet<T> extends AbstractSet<T> {
	
	public static final int DEFAULT_INITIAL_CAPACITY = 8;
	
	static final Object MISSING_VALUE = null;
	
	private final boolean shouldAvoidAllocation;
	private final float loadFactor;
	private int resizeThreshold;
	private int size;
	
	private T[] values;
	private ObjectIterator iterator;
	private IntConsumer resizeNotifier;
	
	public ObjectHashSet() {
		this(DEFAULT_INITIAL_CAPACITY);
	}
	
	public ObjectHashSet(final int proposedCapacity) {
		this(proposedCapacity, Hashing.DEFAULT_LOAD_FACTOR);
	}
	
	public ObjectHashSet(final int proposedCapacity, final float loadFactor) {
		this(proposedCapacity, loadFactor, true);
	}
	
	@SuppressWarnings("unchecked")
	public ObjectHashSet(final int proposedCapacity, final float loadFactor, final boolean shouldAvoidAllocation) {
		validateLoadFactor(loadFactor);
		
		this.shouldAvoidAllocation = shouldAvoidAllocation;
		this.loadFactor = loadFactor;
		size = 0;
		
		final int capacity = findNextPositivePowerOfTwo(Math.max(DEFAULT_INITIAL_CAPACITY, proposedCapacity));
		resizeThreshold = (int) (capacity * loadFactor);
		values = (T[]) new Object[capacity];
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
	
	public void resizeNotifier(final IntConsumer resizeNotifier) {
		this.resizeNotifier = resizeNotifier;
	}
	
	public boolean add(final T value) {
		Objects.requireNonNull(value);
		final int mask = values.length - 1;
		int index = Hashing.hash(value.hashCode(), mask);
		
		while (values[index] != MISSING_VALUE) {
			if (values[index].equals(value)) {
				return false;
			}
			
			index = next(index, mask);
		}
		
		values[index] = value;
		size++;
		
		if (size > resizeThreshold) {
			increaseCapacity();
			if (resizeNotifier != null) {
				resizeNotifier.accept(resizeThreshold);
			}
		}
		
		return true;
	}
	
	private void increaseCapacity() {
		final int newCapacity = values.length << 1;
		if (newCapacity < 0) {
			throw new IllegalStateException("max capacity reached at size=" + size);
		}
		
		rehash(newCapacity);
	}
	
	@SuppressWarnings("unchecked")
	private void rehash(final int newCapacity) {
		final int mask = newCapacity - 1;
		resizeThreshold = (int) (newCapacity * loadFactor);
		
		final T[] tempValues = (T[]) new Object[newCapacity];
		Arrays.fill(tempValues, MISSING_VALUE);
		
		for (final T value : values) {
			if (value != MISSING_VALUE) {
				int newHash = Hashing.hash(value.hashCode(), mask);
				while (tempValues[newHash] != MISSING_VALUE) {
					newHash = ++newHash & mask;
				}
				
				tempValues[newHash] = value;
			}
		}
		
		values = tempValues;
	}
	
	public boolean remove(final Object value) {
		final Object[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(value.hashCode(), mask);
		
		while (values[index] !=  MISSING_VALUE) {
			if (values[index].equals(value)) {
				values[index] = MISSING_VALUE;
				compactChain(index);
				size--;
				return true;
			}
			
			index = next(index, mask);
		}
		
		return false;
	}
	
	private static int next(final int index, final int mask) {
		return (index + 1) & mask;	
	}
	
	@SuppressWarnings("FinalParameters")
	void compactChain(int deleteIndex) {
		final Object[] values = this.values;
		final int mask = values.length - 1;
	
		int index = deleteIndex;
		while (true) {
			index = next(index, mask);
			if (values[index] == MISSING_VALUE) {
				return;
			}
			
			final int hash = Hashing.hash(values[index].hashCode(), mask);
			
			if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) || 
					(hash <= deleteIndex && deleteIndex <= index)) {
				values[deleteIndex] = values[index];
				
				values[index] = MISSING_VALUE;
				deleteIndex = index;
			}
			
		}
	}
	
	public void compact() {
		final int idealCapacity = (int) Math.round(size() * (1.0 / loadFactor));
		rehash(findNextPositivePowerOfTwo(Math.max(DEFAULT_INITIAL_CAPACITY, idealCapacity)));
	}
	
	public boolean contains(final Object value) {
		final int mask = values.length - 1;
		int index = Hashing.hash(value.hashCode(), mask);
		
		while (values[index] != MISSING_VALUE) {
			if (value == values[index] || values[index].equals(value)) {
				return true;
			}
			
			index = next(index, mask);
		}
		
		return false;
	}
	
	public int size() {
		return size;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public void clear() {
		if (size > 0) {
			Arrays.fill(values, MISSING_VALUE);
			size = 0;
		} 
	}
	
	public boolean containsAll(final Collection<?> col1) {
		for (final Object t : col1) {
			if (!contains(t)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean addAll(final Collection<? extends T> col1) {
		boolean acc = false;
		for (final T t : col1) {
			acc |= add(t);
		}
		
		return acc;
	}
	
	public boolean addAll(final ObjectHashSet<T> col1) {
		boolean acc = false;
		
		for (final T value : col1.values) {
			if (value != MISSING_VALUE) {
				acc |= add(value);
			}
		}
		
		return acc;
	}
	
	public ObjectHashSet<T> difference(final ObjectHashSet<T> other) {
		ObjectHashSet<T> difference = null;
		
		for (final T value : other.values) {
			if (value != MISSING_VALUE && (!contains(value))) {
				if (difference == null) {
					difference = new ObjectHashSet<>(size);
				}
				
				difference.add(value);
			}
		}
		
		return difference;
	}
	
	public boolean removeAll(final Collection<?> col1) {
		boolean acc = false;
		
		for (final Object t : col1) {
			acc |= remove(t);
		}
		
		return acc;
	}
	
	public boolean removeAll(final ObjectHashSet<T> col1) {
		boolean acc = false;
		
		for (final T value : col1.values) {
			if (value != MISSING_VALUE) {
				acc |= remove(value);
			}
		}
		
		return acc;
	}
	
	public ObjectIterator iterator() {
		ObjectIterator iterator = this.iterator;
		if (null == iterator) {
			iterator = new ObjectIterator();
			
			if (shouldAvoidAllocation) {
				this.iterator = iterator;
			}
		}
		
		return iterator.reset();
	}
	
	public void copy(final ObjectHashSet<T> that) {
		if (this.values.length != that.values.length) {
			throw new IllegalArgumentException("cannot copy object: lengths not equal");
		}
		
		System.arraycopy(that.values, 0, this.values, 0, this.values.length);
		this.size = that.size;
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append('{');
		
		for (final Object value : values) {
			if (value != MISSING_VALUE) {
				sb.append(value).append(", ");
			}
		}
		
		if (sb.length() > 1) {
			sb.setLength(sb.length() - 2);
		}
		
		sb.append('}');
		
		return sb.toString();
	}
	
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		
		if (other instanceof ObjectHashSet) {
			final ObjectHashSet<?> otherSet = (ObjectHashSet<?>)other;
			return otherSet.size == size && containsAll(otherSet);
		}
		
		if (!(other instanceof Set) ) {
			return false;
		}
		
		final Set<?> c = (Set<?>) other;
		
		if (c.size() != size()) {
			return false;
		}
		
		try {
			return containsAll(c);
		} catch (final ClassCastException | NullPointerException ignore) {
			return false;
		}
	}
	
	public int hashCode() {
		int hashCode = 0;
		
		for (final Object value : values) {
			if (value != MISSING_VALUE) {
				hashCode += value.hashCode();
			}
		}
		
		return hashCode;
	}
	
	public void forEach(final Consumer<? super T> action) {
		int remaining = size;
		
		for (int i = 0, length = values.length; remaining > 0 && i < length; i++) {
			if (null != values[i]) {
				action.accept(values[i]);
				--remaining;
			}
		}
	}
	
	public final class ObjectIterator implements Iterator<T> {
		private int remaining;
		private int positionCounter;
		private int stopCounter;
		private boolean isPositionValid = false;
		
		public ObjectIterator() {
			
		}
		
		ObjectIterator reset() {
			this.remaining = size;
			final T[] values = ObjectHashSet.this.values;
			final int length = values.length;
			int i = length;
			
			if (values[length - 1] != MISSING_VALUE) {
				i = 0;
				for (; i < length; i++) {
					if (values[i] == MISSING_VALUE) {
						break;
					}
				}
			}
			
			stopCounter = i;
			positionCounter = i + length;
			isPositionValid = false;
			return this;
		}
		
		public int remaining() {
			return remaining;
		}
		
		public boolean hasNext() {
			return remaining > 0;
		}
		
		public T next() {
			return nextValue();
		}
		
		public T nextValue() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			
			final T[] values = ObjectHashSet.this.values;
			final int mask = values.length - 1;
			
			for (int i = positionCounter - 1; i >= stopCounter; i--) {
				final int index = i & mask;
				final T value = values[index];
				if (value != MISSING_VALUE) {
					positionCounter = i;
					isPositionValid = true;
					--remaining;
					
					return value;
				}
			}
			
			throw new IllegalStateException();
		}
		
		@SuppressWarnings("unchecked")
		public void remove() {
			if (isPositionValid) {
				final T[] values = ObjectHashSet.this.values;
				final int position = position(values);
				values[position] = (T)MISSING_VALUE;
				--size;
				
				compactChain(position);
				
				isPositionValid = false;
			} else {
				throw new IllegalStateException();
			}
		}
		
		private int position(final T[] values) {
			return positionCounter & (values.length - 1);
		}
	}
}
