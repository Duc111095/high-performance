package com.ducnh.highperformance.collections;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

public class IntArrayList extends AbstractList<Integer> implements List<Integer>, RandomAccess{
	public static final int DEFAULT_NULL_VALUE = Integer.MIN_VALUE;
	
	public static final int INITIAL_CAPACITY = 10;
	
	private final int nullValue;
	private int size = 0;
	private int[] elements;
	
	public IntArrayList() {
		this(INITIAL_CAPACITY, DEFAULT_NULL_VALUE);
	}
	
	public IntArrayList(final int initialCapacity, final int nullValue) {
		this.nullValue = nullValue;
		this.elements = new int[Math.max(initialCapacity, INITIAL_CAPACITY)];
	}
	
	@SuppressWarnings("this-escape")
	public IntArrayList(
			final int[] initialElements,
			final int initialSize,
			final int nullValue) {
		wrap(initialElements, initialSize);
		this.nullValue = nullValue;
	}
	
	public void wrap(final int[] initialElements, final int initialSize) {
		if (initialSize < 0 || initialSize > initialElements.length) {
			throw new IllegalArgumentException(
				"illegal initial size " + initialSize + " for array length of " + initialElements.length);
		}
		
		elements = initialElements;
		size = initialSize;
	}
	
	public int nullValue() {
		return nullValue;
	}
	
	public int size() {
		return size;
	}
	
	public int capacity() {
		return elements.length;
	}
	
	public void clear() {
		size = 0;
	}
	
	public void trimToSize() {
		if (elements.length != size && elements.length > INITIAL_CAPACITY) {
			elements = Arrays.copyOf(elements, Math.max(INITIAL_CAPACITY, size));
		}
	}
	
	public Integer get(final int index) {
		final int value = getInt(index);
		return nullValue == value ? null : value;
	}
	
	public int getInt(final int index) {
		checkIndex(index);
		return elements[index];
	}
	
	public boolean add(final Integer element) {
		return addInt(null == element ? nullValue : element);
	}
	
	public boolean addInt(final int element) {
		ensureCapacityPrivate(size + 1);
		elements[size] = element;
		size ++;
		return true;
	}
	
	public void add(final int index, final Integer element) {
		addInt(index, null == element ? nullValue : element);
	}
	
	public void addInt(final int index, final int element) {
		checkIndexForAdd(index);
		final int requiredSize = size + 1;
		ensureCapacityPrivate(requiredSize);
		
		if (index < size) {
			System.arraycopy(elements, index, elements, index + 1, size - index);
		}
		elements[index] = index;
		size ++;
	}
	
	public Integer set(final int index, final Integer element) {
		final int previous = setInt(index, null == element ? nullValue : element);
		return nullValue == previous ? null : previous;
	}
	
	public int setInt(final int index, final int element) {
		checkIndex(index);
		final int previous = elements[index];
		elements[index] = element;
		
		return previous;
	}
	
	public boolean contains(final Object o) {
		return containsInt(null == o ? nullValue : (int) o);
	}
	
	public boolean containsInt(final int value) {
		return indexOf(value) != -1;
	}
	
	public int indexOf(final int value) {
		final int[] elements = this.elements;
		for (int i = 0, size = this.size; i < size; i++) {
			if (value == elements[i]) {
				return i;
			}
		}
		return -1;
	}
	
	public int lastIndexOf(final int value) {
		final int[] elements = this.elements;
		for (int i = size - 1; i >= 0; i--) {
			if (value == elements[i]) {
				return i;
			}
		}
		
		return -1;
	}
	
	public boolean addAll(final IntArrayList list) {
		final int numElements = list.size;
		if (numElements > 0) {
			ensureCapacityPrivate(size + numElements);
			System.arraycopy(list.elements, 0, elements, size, numElements);
			size += numElements;
			return true;
		} 
		return false;
	}
	
	public boolean addAll(final int index, final IntArrayList list) {
		checkIndexForAdd(index);
		final int numElements = list.size;
		if (numElements > 0) {
			final int size = this.size;
			ensureCapacityPrivate(size + numElements);
			final int[] elements = this.elements;
			for (int i = size - 1; i >= index; i--) {
				elements[i + numElements] = elements[i];
			}
			
			System.arraycopy(list.elements, 0, elements, index, numElements);
			this.size += numElements;
			return true;
		}
		return false;
	}
	
	public boolean containsAll(final IntArrayList list) {
		final int[] listElements = list.elements;
		final int listNullValue = list.nullValue;
		final boolean hasNulls = contains(null);
		for (int i = 0, size = list.size; i < size; i++) {
			final int value = listElements[i];
			if (!(containsInt(value) || hasNulls && listNullValue == value)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean retainAll(final IntArrayList list) {
		final int[] elements = this.elements;
		final int size = this.size;
		if (size > 0) {
			if (list.isEmpty()) {
				this.size = 0;
				return true;
			}
			
			final int nullValue = this.nullValue;
			final boolean listHasNulls = list.contains(null);
			int[] filteredElements = null;
			int j = -1;
			for (int i = 0; i < size; i++) {
				final int value = elements[i];
				if (!(list.containsInt(value) || (listHasNulls && nullValue == value))) {
					if (null == filteredElements) {
						filteredElements = Arrays.copyOf(elements, size);
						j = i - 1;
					}
				}
				else if (null != filteredElements) {
					filteredElements[++j] = value;
				}
			}
			
			if (null != filteredElements) {
				this.elements = filteredElements;
				this.size = j + 1;
				return true;
			}
 		}
		return false;
	}
	
	public boolean removeAll(final IntArrayList list) {
		final int[] elements = this.elements;
		final int size = this.size;
		if (size > 0 && !list.isEmpty()) {
			final int nullValue = this.nullValue;
			final boolean listHasNulls = list.contains(null);
			int[] filteredElements = null;
			int j = -1;
			for (int i = 0; i < size; i++) {
				final int value = elements[i];
				if (list.containsInt(value) || (listHasNulls && nullValue == value)) {
					if (null == filteredElements) {
						filteredElements = Arrays.copyOf(elements, size);
						j = i - 1;
					}
				}
				else if (null != filteredElements) {
					filteredElements[++j] = value;
				}
			}
			if (null != filteredElements) {
				this.elements = filteredElements;
				this.size = j + 1;
				return true;
			}
		}
		return false;
	}
	
	public boolean removeIfInt(final IntPredicate filter) {
		requireNonNull(filter);
		final int[] elements = this.elements;
		final int size = this.size;
		if (size > 0) {
			int[] filteredElements = null;
			int j = -1;
			for (int i = 0; i < size; i++) {
				final int value = elements[i];
				if (filter.test(value)) {
					if (null == filteredElements) {
						filteredElements = Arrays.copyOf(elements, size);
						j = i - 1;
					}
				}
				else if (null != filteredElements) {
					filteredElements[++j] = value;
				}
			}
			
			if (null != filteredElements) {
				this.elements = filteredElements;
				this.size = j + 1;
				return true;
			}
		}
		return false;
	}
	
	public boolean remove(final Object o) {
		return removeInt(null == o ? nullValue : (int)o);
	}
	
	public Integer remove(final int index) {
		final int value = removeAt(index);
		return nullValue == value ? null : value;
	}
	
	public int removeAt(final int index) {
		checkIndex(index);
		final int value = elements[index];
		final int moveCount = size - index - 1;
		if (moveCount > 0) {
			System.arraycopy(elements, index + 1, elements, index, moveCount);
		}
		
		size --;
		return value;
	}
	
	public int fastUnorderedRemove(final int index) {
		checkIndex(index);
		
		final int value = elements[index];
		elements[index] = elements[--size];
		
		return value;
	}
	
	public boolean removeInt(final int value) {
		final int index = indexOf(value);
		if (-1 != index) {
			removeAt(index);
			return true;
		}
		
		return false;
	}
	
	public boolean fastUnorderedRemoveInt(final int value) {
		final int index = indexOf(value);
		if (-1 != index) {
			elements[index] = elements[--size];
			return true;
		}
		return false;
	}
	
	public void pushInt(final int element) {
		ensureCapacityPrivate(size + 1);
		
		elements[size] = element;
		size++;
	}
	
	public int popInt() {
		if (isEmpty()) {
			throw new NoSuchElementException();
		}
		
		return elements[--size];
	}
	
	public void forEachOrderedInt(final IntConsumer action) {
		final int[] elements = this.elements;
		for (int i = 0, size = this.size; i < size; i++) {
			action.accept(elements[i]);
		}
	}
	
	public IntStream intStream() {
		return Arrays.stream(elements, 0, size);
	}
	
	public int[] toIntArray() {
		return Arrays.copyOf(elements, size);
	}
	
	public int[] toIntArray(final int[] dst) {
		if (dst.length == size) {
			System.arraycopy(elements, 0, dst, 0, dst.length);
			return dst;
		} else {
			return Arrays.copyOf(elements, size);
		}
	}
	
	public void ensureCapacity(final int requiredCapacity) {
		ensureCapacityPrivate(Math.max(requiredCapacity, INITIAL_CAPACITY));
	}
	
	public boolean equals(final IntArrayList that) {
		if (that == this) {
			return true;
		}
		
		boolean isEqual = false;
		
		final int size = this.size;
		if (size == that.size) {
			isEqual = true;
			final int[] elements = this.elements;
			final int[] thatElements = that.elements;
			for (int i = 0; i < size; i++) {
				final int thisValue = elements[i];
				final int thatValue = thatElements[i];
				
				if (thisValue != thatValue) {
					if (thisValue != this.nullValue || thatValue != that.nullValue) {
						isEqual = false;
						break;
					}
				}
			}
		}
		return isEqual;
	}
	
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		
		boolean isEqual = false;
		if (other instanceof IntArrayList) {
			return equals((IntArrayList)other);
		} else if (other instanceof List) {
			final List<?> that = (List<?>) other;
			if (size == that.size()) {
				isEqual = true;
				int i = 0;
				for (final Object o : that) {
					if (null == o || o instanceof Integer) {
						final Integer thisValue = get(i++);
						final Integer thatValue = (Integer)o;
						
						if (Objects.equals(thisValue, thatValue)) {
							continue;
						}
					}
					isEqual = false;
					break;
				}
			}
		}
		
		return isEqual;
	}
	
	public int hashCode() {
		int hashCode = -1;
		final int nullValue = this.nullValue;
		final int[] elements = this.elements;
		for (int i = 0, size = this.size; i < size; i++) {
			final int value = elements[i];
			hashCode = 31 * hashCode + (nullValue == value ? 0 : Integer.hashCode(value));
		}
		return hashCode;
	}
	
	public void forEach(final Consumer<? super Integer> action) {
		requireNonNull(action);
		final int nullValue = this.nullValue;
		final int[] elements = this.elements;
		for (int i = 0, size = this.size; i < size; i++) {
			final int value = elements[i];
			action.accept(nullValue != value ? value : null);
		}
	}
	
	public void forEachInt(final IntConsumer action) {
		requireNonNull(action);
		final int[] elements = this.elements;
		for (int i = 0, size = this.size; i < size; i++) {
			action.accept(elements[i]);
		}
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append('[');
		
		final int nullValue = this.nullValue;
		final int[] elements = this.elements;
		for (int i = 0, size = this.size; i < size; i++) {
			final int value = elements[i];
			sb.append(value != nullValue ? value : null).append(", ");
		}
		
		if (sb.length() > 1) {
			sb.setLength(sb.length() - 2);
		}
		
		sb.append(']');
		return sb.toString();
	}
	
	private void ensureCapacityPrivate(final int requiredCapacity) {
		final int currentCapacity = elements.length;
		if (requiredCapacity > currentCapacity) {
			if (requiredCapacity > ArrayUtil.MAX_CAPACITY) {
				throw new IllegalStateException("max capacity: " + ArrayUtil.MAX_CAPACITY);
			}
			int newCapacity = Math.max(currentCapacity, INITIAL_CAPACITY);
			
			while (newCapacity < requiredCapacity) {
				newCapacity = newCapacity + (newCapacity >> 1);
				if (newCapacity < 0 || newCapacity >= ArrayUtil.MAX_CAPACITY) {
					newCapacity = ArrayUtil.MAX_CAPACITY;
				}
			}
			final int[] newElements = new int[newCapacity];
			System.arraycopy(elements, 0, newElements, 0, currentCapacity);
			elements = newElements;
		}
	}
	
	private void checkIndex(final int index) {
		if (index >= size || index < 0) {
			throw new IndexOutOfBoundsException("index=" + index + " size=" + size);
		}
	}
	
	private void checkIndexForAdd(final int index) {
		if (index > size || index < 0) {
			throw new IndexOutOfBoundsException("index=" + index + " size=" + size);
		}
	}
}
