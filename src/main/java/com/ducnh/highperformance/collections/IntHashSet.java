package com.ducnh.highperformance.collections;

import static com.ducnh.highperformance.BitUtil.findNextPositivePowerOfTwo;
import static com.ducnh.highperformance.collections.CollectionUtil.validateLoadFactor;

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import com.ducnh.highperformance.collections.IntArrayQueue.IntIterator;

public class IntHashSet extends AbstractSet<Integer>{
	
	public static final int DEFAULT_INITIAL_CAPACITY = 8;
	static final int MISSING_VALUE = 0;
	
	private final boolean shouldAvoidAllocation;
	private boolean containsMissingValue;
	private final float loadFactor;
	private int resizeThreshold;
	private int sizeOfArrayValues;
	
	private int[] values;
	private IntIterator iterator;
	
	public IntHashSet() {
		this(DEFAULT_INITIAL_CAPACITY);
	}
	
	public IntHashSet(
			final int proposedCapacity) {
		this(proposedCapacity, Hashing.DEFAULT_LOAD_FACTOR, true);
	}
	
	public IntHashSet(
			final int proposedCapacity,
			final float loadFactor) {
		this(proposedCapacity, loadFactor, true);
	}
	
	public IntHashSet(
			final int proposedCapacity, 
			final float loadFactor,
			final boolean shouldAvoidAllocation) {
		validateLoadFactor(loadFactor);
		
		this.shouldAvoidAllocation = shouldAvoidAllocation;
		this.loadFactor = loadFactor;
		sizeOfArrayValues = 0;
		final int capacity = findNextPositivePowerOfTwo(Math.max(DEFAULT_INITIAL_CAPACITY, proposedCapacity));
		resizeThreshold = (int)(capacity * loadFactor);
		values = new int[capacity];
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
	
	public boolean add(final Integer value) {
		return add(value.intValue());
	}
	
	public boolean add(final int value) {
		if (MISSING_VALUE == value) {
			final boolean previousContainsMissingValue = this.containsMissingValue;
			containsMissingValue = true;
			return !previousContainsMissingValue;
		}
		
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(value, mask);
		
		int oldValue;
		while (MISSING_VALUE != (oldValue = values[index])) {
			if (oldValue == value) {
				return false;
			}
			
			index = next(index, mask);
		}
		
		values[index] = value;
		sizeOfArrayValues++;
		
		if (sizeOfArrayValues > resizeThreshold) {
			increaseCapacity();
		}
		
		return true;
	}
	
	private void increaseCapacity() {
		final int newCapacity = values.length * 2;
		if (newCapacity < 0) {
			throw new IllegalStateException("max capacity reached at size=" + size());
		}
		
		rehash(newCapacity);
	}
	
	private void rehash(final int newCapacity) {
		final int capacity = newCapacity;
		final int mask = newCapacity - 1;
		resizeThreshold = (int)(newCapacity * loadFactor);
		
		final int[] tempValues = new int[capacity];
		final int[] values = this.values;
		for (final int value : values) {
			if (MISSING_VALUE != value) {
				int newHash =  Hashing.hash(value, mask);
				while (MISSING_VALUE != tempValues[newHash]) {
					newHash = ++newHash & mask;
				}
				
				tempValues[newHash] = value;
			}
		}
		
		this.values = tempValues;
	}
	
	public boolean remove(final Object value) {
		return remove((int) value);
	}
	
	public boolean remove(final int value) {
		if (MISSING_VALUE == value) {
			final boolean previousContainsMissingValue = this.containsMissingValue;
			containsMissingValue = false;
			return previousContainsMissingValue;
		}
		
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = Hashing.hash(value, mask);
		
		int oldValue;
		while (MISSING_VALUE != (oldValue = values[index])) {
			if (oldValue == value) {
				values[index] = MISSING_VALUE;
				sizeOfArrayValues--;
				compactChain(index);
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
		final int[] values = this.values;
		final int mask = values.length - 1;
		int index = deleteIndex;
		
		while (true) {
			index = next(index, mask);
			final int value = values[index];
			if (MISSING_VALUE == value) {
				return;
			}
			
			final int hash = Hashing.hash(value, mask);
			
			if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) || 
					(hash <= deleteIndex && deleteIndex <= index)) {
				values[deleteIndex] = value;
				
				values[index] = MISSING_VALUE;
				deleteIndex = index;
			}
		}
	}
	
	public void compact() {
		final int idealCapacity = (int)Math.round(size() * (1.0 / loadFactor));
		rehash(findNextPositivePowerOfTwo(Math.max(DEFAULT_INITIAL_CAPACITY, idealCapacity)));
	}
	
	public boolean contains(final Object value) {
		return contains((int) value);
	}
	
	public boolean contains(final int value) {
		if (MISSING_VALUE == value) {
			return containsMissingValue;
		}
		
		final int mask = values.length - 1;
		int index = Hashing.hash(value, mask);
		
		int existingValue;
		while (MISSING_VALUE != (existingValue = values[index])) {
			if (existingValue == value) {
				return true;
			}
			
			index = next(index, mask);
		}
		
		return false;
	}
	
	public int size() {
		return sizeOfArrayValues + (containsMissingValue ? 1 : 0);
	}
	
	public boolean isEmpty() {
		return size() == 0;
	}
	
	public void clear() {
		if (size() > 0) {
			Arrays.fill(values, MISSING_VALUE);
			sizeOfArrayValues = 0;
			containsMissingValue = false;
		}
	}
	
	public boolean addAll(final Collection<? extends Integer> col1) {
		boolean added = false;
		
		for (final Integer value : col1) {
			added |= add(value);
		}
		
		return added;
	}
	
	public boolean addAll(final IntHashSet col1) {
		boolean acc = false;
		
		for (final int value : col1.values) {
			if (MISSING_VALUE != value) {
				acc |= add(value);
			}
		}
		
		if (col1.containsMissingValue) {
			acc |= add(MISSING_VALUE);
		}
		
		return acc;
	}
	
	public boolean containsAll(final IntHashSet col1) {
		for (final int value : col1.values) {
			if (MISSING_VALUE != value && !contains(value)) {
				return false;
			}
		}
		
		return containsMissingValue || !col1.containsMissingValue;
	}
	
	public IntHashSet differenee (final IntHashSet other) {
		IntHashSet difference = null;
		
		final int[] values = this.values;
		for (final int value : values) {
			if (MISSING_VALUE != value && !other.contains(value)) {
				if (difference == null) {
					difference = new IntHashSet();
				}
				
				difference.add(value);
			}
		}
		
		if (containsMissingValue && !other.containsMissingValue) {
			if (difference == null) {
				difference = new IntHashSet();
			}
			
			difference.add(MISSING_VALUE);
		}
		
		return difference;
	}
	
	public boolean removeIf(final Predicate<? super Integer> filter) {
		return super.removeIf(filter);
	}
	
	public boolean removeIfInt(final IntPredicate filter) {
		boolean removed = false;
		final IntIterator iterator = iterator();
		while (iterator.hasNext()) {
			if (filter.test(iterator.nextValue())) {
				iterator.remove();
				removed = true;
			}
		}
		
		return removed;
	}
	
	public boolean removeAll(final IntHashSet col1) {
		boolean removed = false;
		for (final int value : col1.values) {
			if (MISSING_VALUE != value) {
				removed |= remove(value);
			}
		} 
		
		if (col1.containsMissingValue) {
			removed |= remove(MISSING_VALUE);
		}
		
		return removed;
	}
	
	public boolean retainAll(final Collection<?> col1) {
		boolean removed = false;
		final int[] values = this.values;
		final int length = values.length;
		int i = 0;
		for (; i < length; i++) {
			final int value = values[i];
			if (MISSING_VALUE != value && !col1.contains(value)) {
				values[i] = MISSING_VALUE;
				sizeOfArrayValues--;
				removed = true;
			}
		}
		
		if (removed && sizeOfArrayValues > 0) {
			rehash(values.length);
		}
		
		if (containsMissingValue && !col1.contains(MISSING_VALUE)) {
			containsMissingValue = false;
			removed = true;
		}
		
		return removed;
	}
	
	public boolean retainAll(final IntHashSet col1) {
		boolean removed = false;
		final int length = values.length;
		int i = 0;
		for (; i < length; i++) {
			final int value = values[i];
			if (MISSING_VALUE != value && !col1.contains(value)) {
				values[i] = MISSING_VALUE;
				sizeOfArrayValues--;
				removed = true;
			}
		}
		
		if (removed && sizeOfArrayValues > 0) {
			rehash(values.length);
		} 
		
		if (containsMissingValue && !col1.containsMissingValue) {
			containsMissingValue = false;
			removed = true;
		}
		
		return removed;
	}
	
	public IntIterator iterator() {
		IntIterator iterator = this.iterator;
		if (iterator == null) {
			iterator = new IntIterator();
			if (shouldAvoidAllocation) {
				this.iterator = iterator;
			}
		}
		
		return iterator.reset();
	}
	
	public void forEacHInt(final IntConsumer action) {
		if (sizeOfArrayValues > 0) {
			final int[] values = this.values;
			for (final int v : values) {
				if (MISSING_VALUE != v) {
					action.accept(v);
				}
			}
		}
		
		if (containsMissingValue) {
			action.accept(MISSING_VALUE);
		}
	}
	
	public void copy(final IntHashSet that) {
		if (values.length != that.values.length) {
			throw new IllegalArgumentException("cannot copy object: masks not equal");
		}
		
		System.arraycopy(that.values, 0, values, 0, values.length);
		this.sizeOfArrayValues = that.sizeOfArrayValues;
		this.containsMissingValue = that.containsMissingValue;
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append('{');
		
		final int[] values = this.values;
		for (final int value : values) {
			if (MISSING_VALUE != value) {
				sb.append(value).append(", ");
			}
		}
		
		if (containsMissingValue) {
			sb.append(MISSING_VALUE).append(", ");
		}
		
		if (sb.length() > 1) {
			sb.setLength(sb.length() - 2);
		}
		
		sb.append('}');
		
		return sb.toString();
	}
	
	public <T> T[] toArray(final T[] a) {
		final Class<?> componentType = a.getClass().getComponentType();
		if (!componentType.isAssignableFrom(Integer.class)) {
			throw new ArrayStoreException("cannot store Integers in array of type " + componentType);
		}
		
		final int size = size();
		final T[] arrayCopy = a.length >= size ? a : (T[]) Array.newInstance(componentType, size);
		copyValues(arrayCopy);
		
		return arrayCopy;
	}
	
	public Object[] toArray() {
		final Object[] arrayCopy = new Object[size()];
		copyValues(arrayCopy);
		
		return arrayCopy;
	}
	
	private void copyValues(final Object[] arrayCopy) {
		int i = 0;
		final int[] values = this.values;
		for (final int value : values) {
			if (MISSING_VALUE != value) {
				arrayCopy[i++] = value;
			}
		}
		
		if (containsMissingValue) {
			arrayCopy[sizeOfArrayValues] = MISSING_VALUE;
		}
	}
	
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		
		if (other instanceof IntHashSet) {
			final IntHashSet otherSet = (IntHashSet) other;
			
			return otherSet.containsMissingValue == containsMissingValue &&
					otherSet.sizeOfArrayValues == sizeOfArrayValues &&
					containsAll(otherSet);
		}
		
		if (!(other instanceof Set)) {
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
		for (final int value : values) {
			if (MISSING_VALUE != value) {
				hashCode += Integer.hashCode(value);
			}
		}
		
		if (containsMissingValue) {
			hashCode += Integer.hashCode(MISSING_VALUE);
		}
		
		return hashCode;
	}
	
	public final class IntIterator implements Iterator<Integer> {
		private int remaining;
		private int positionCounter;
		private int stopCounter;
		private boolean isPositionValid = false;
	
		public IntIterator() {
			
		}
		
		IntIterator reset() {
			remaining = size();
			final int[] values = IntHashSet.this.values;
			final int length = values.length;
			int i = length;
			
			if (MISSING_VALUE != values[length - 1]) {
				for (i = 0; i < length; i++) {
					if (MISSING_VALUE == values[i]) {
						break;
					}
				}
			}
			
			stopCounter = i;
			positionCounter = i + length;
			isPositionValid = false;
			
			return this;
		}
		
		public boolean hasNext() {
			return remaining > 0;
		}
		
		public int remaining() {
			return remaining;
		}
		
		public Integer next() {
			return nextValue();
		}
		
		public int nextValue() {
			if (remaining == 1 && containsMissingValue) {
				remaining = 0;
				isPositionValid = true;
				
				return MISSING_VALUE;
			}
			
			findNext();
			
			final int[] values = IntHashSet.this.values;
			
			return values[position(values)];
		}
		
		public void remove() {
			if (isPositionValid) {
				if (0 == remaining && containsMissingValue) {
					containsMissingValue = false;
				} else {
					final int[] values = IntHashSet.this.values;
					final int position = position(values);
					values[position] = MISSING_VALUE;
					--sizeOfArrayValues;
					
					compactChain(position);
				}
				
				isPositionValid = false;
			} else {
				throw new IllegalStateException();
			}
		}
		
		private void findNext() {
			final int[] values = IntHashSet.this.values;
			final int mask = values.length - 1;
			isPositionValid = true;
			
			for (int i = positionCounter - 1, stop = stopCounter;  i >= stop; i--) {
				final int index = i & mask;
				if (MISSING_VALUE != values[index]) {
					positionCounter = i;
					--remaining;
					return;
				}
			}
			
			isPositionValid = false;
			throw new NoSuchElementException();
		}
		
		private int position(
				final int[] values) {
			return positionCounter & (values.length - 1);
		}
	}
} 
