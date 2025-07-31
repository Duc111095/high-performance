package com.ducnh.highperformance.collections;

import static com.ducnh.highperformance.BitUtil.findNextPositivePowerOfTwo;
import static com.ducnh.highperformance.collections.CollectionUtil.validateLoadFactor;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
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
}
