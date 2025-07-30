package com.ducnh.highperformance.collections;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.ducnh.highperformance.BitUtil;

public class IntArrayQueue extends AbstractQueue<Integer>{
	
	public static final int DEFAULT_NULL_VALUE = Integer.MIN_VALUE;
	
	public static final int MIN_CAPACITY = 8;
	private final boolean shouldAvoidAllocation;
	private int head;
	private int tail;
	private final int nullValue;
	private int[] elements;
	private IntIterator iterator;
	
	public IntArrayQueue() {
		this(MIN_CAPACITY, DEFAULT_NULL_VALUE, true);
	}
	
	public IntArrayQueue(final int nullValue) {
		this(MIN_CAPACITY, nullValue, true);
	}
	
	public IntArrayQueue(
			final int initialCapacity,
			final int nullValue) {
		this(initialCapacity, nullValue, true);
	}
	
	public IntArrayQueue(
			final int initialCapacity,
			final int nullValue, 
			final boolean shouldAvoidAllocation) {
		this.nullValue = nullValue;
		this.shouldAvoidAllocation = shouldAvoidAllocation;
		
		if (initialCapacity < MIN_CAPACITY) {
			throw new IllegalArgumentException("initial capacity < MIN_INITIAL_CAPACITY : " + initialCapacity);
		}
		
		final int capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
		if (capacity < MIN_CAPACITY) {
			throw new IllegalArgumentException("invalid initial capacity: " + initialCapacity);
		}
		
		elements = new int[capacity];
		Arrays.fill(elements, nullValue);
	}
	
	public int nullValue() {
		return nullValue;
	}
	
	public int capacity() {
		return elements.length;
	}
	
	public int size() {
		return (tail - head) & (elements.length - 1);
	}
	
	public boolean isEmpty() {
		return head == tail;
	}
	
	public void clear() {
		if (head != tail) {
			Arrays.fill(elements, nullValue);
			head = 0;
			tail = 0;
		}
	}
	
	public boolean offer(final Integer element) {
		return offerInt(element);
	}
	
	public boolean offerInt(final int element) {
		if (nullValue == element) {
			throw new NullPointerException();
		}
		
		elements[tail] = element;
		tail = (tail + 1) & (elements.length - 1);
		
		if (tail == head) {
			increaseCapacity();		
		}
		
		return true;
	}
	
	public boolean add(final Integer element) {
		return offerInt(element);
	}
	
	public boolean addInt(final int element) {
		return offerInt(element);
	}
	
	public Integer peek() {
		final int element = elements[head];
		
		return element == nullValue ? null : element;
	}
	
	public int peekInt() {
		return elements[head];
	}
	
	public Integer poll() {
		final int element = pollInt();
		
		return element == nullValue ? null : element;
	}
	
	public int pollInt() {
		final int element = elements[head];
		if (nullValue == element) {
			return nullValue;
		}
		
		elements[head] = nullValue;
		head = (head + 1) & (elements.length - 1);
		
		return element;
	}
	
	public Integer remove() {
		final int element = pollInt();
		if (nullValue == element) {
			throw new NoSuchElementException();
		}
		
		return element;
	}
	
	public Integer element() {
		final int element = elements[head];
		if (nullValue == element) {
			throw new NoSuchElementException();
		}
		
		return element;
	}
	
	public int elementInt() {
		final int element = elements[head];
		if (nullValue == element) {
			throw new NoSuchElementException();
		}
		
		return element;
	}
	
	public int removeInt() {
		final int element = pollInt();
		if (nullValue == element) {
			throw new NoSuchElementException();
		}
		
		return element;
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append('[');
		
		for (int i = head; i != tail; ) {
			sb.append(elements[i]).append(", ");
			i = (i + 1) & (elements.length - 1);
		}
		
		if (sb.length() > 1) {
			sb.setLength(sb.length() - 2);
		}
		
		sb.append(']');
		
		return sb.toString();
	}
	
	public void forEach(final Consumer<? super Integer> action) {
		for (int i = head; i != tail; ) {
			action.accept(elements[i]);
			i = (i + 1) & (elements.length - 1);
		}
	}
	
	public void forEachInt(final IntConsumer action) {
		for (int i = head; i != tail; ) {
			action.accept(elements[i]);
			i = (i + 1) & (elements.length - 1);
		}
	}
	
	public IntIterator iterator() {
		IntIterator iterator = this.iterator();
		if (null == iterator) {
			iterator = new IntIterator();
			if (shouldAvoidAllocation) {
				this.iterator = iterator;
			}
		}
		return iterator.reset();
	}
	
	private void increaseCapacity() {
		final int oldHead = head;
		final int oldCapacity = elements.length;
		final int toEndOfArray = oldCapacity - oldHead;
		final int newCapacity = oldCapacity << 1;
		
		if (newCapacity < MIN_CAPACITY) {
			throw new IllegalStateException("max capacity reached");
		}
		
		final int[] array = new int[newCapacity];
		Arrays.fill(array, oldCapacity, newCapacity, nullValue);
		System.arraycopy(elements, oldHead, array, 0, toEndOfArray);
		System.arraycopy(elements, 0, array, toEndOfArray, oldHead);
		
		elements = array;
		head = 0;
		tail = oldCapacity;
	}
	
	public final class IntIterator implements Iterator<Integer> {
		
		private int index;
		
		public IntIterator() {
			
		}
		
		IntIterator reset() {
			index = IntArrayQueue.this.head;
			return this;
		}
		
		public boolean hasNext() {
			return index != tail;
		}
		
		public Integer next() {
			return nextValue();
		}
		
		public int nextValue() {
			if (index == tail) {
				throw new NoSuchElementException();
			}
			
			final int element = elements[index];
			index = (index + 1) & (elements.length - 1);
			
			return element;
		}
		
		
	}
} 
