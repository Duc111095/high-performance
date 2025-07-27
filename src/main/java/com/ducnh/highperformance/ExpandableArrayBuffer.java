package com.ducnh.highperformance;

import static com.ducnh.highperformance.BufferUtil.ARRAY_BASE_OFFSET;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ExpandableArrayBuffer extends AbstractMutableDirectBuffer {
	public static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;
	public static final int INITIAL_CAPACITY = 128;
	
	public ExpandableArrayBuffer() {
		this(INITIAL_CAPACITY);
	}
	
	public ExpandableArrayBuffer(final int initialCapacity) {
		byteArray = new byte[initialCapacity];
		capacity = initialCapacity;
		addressOffset = ARRAY_BASE_OFFSET;
	}
	
	public void wrap(final byte[] buffer) {
		throw new UnsupportedOperationException();
	}
	
	public void wrap(final byte[] buffer, final int offset, final int length) {
		throw new UnsupportedOperationException();
	}
	
	public void wrap(final ByteBuffer buffer) {
		throw new UnsupportedOperationException();
	}
	
	
	public void wrap(final ByteBuffer buffer, final int offset, final int length) {
		throw new UnsupportedOperationException();
	}
	
	public void wrap(final DirectBuffer buffer) {
		throw new UnsupportedOperationException();
	}
	
	public void wrap(final DirectBuffer buffer, final int offset, final int length) {
		throw new UnsupportedOperationException();
	}
	
	public void wrap(final long address, final int length) {
		throw new UnsupportedOperationException();
	}
	
	public ByteBuffer byteBuffer() {
		return null;
	}
	
	public boolean isExpandable() {
		return true;
	}
	
	public int wrapAdjustment() {
		return 0;
	}
	
	public void checkLimit(final int limit) {
		ensureCapacity(limit, 0);
	}
	
	public String toString() {
		return "ExpandableArrayBuffer{" + ", capacity=" + capacity
				+ ", byteArray=" + (byteArray == null ? "null" : ("byte[" + byteArray.length + "]")) 
				+ '}'; 
	}
	
	protected final void ensureCapacity(final int index, final int length) {
		if (index < 0 || length < 0) {
			throw new IndexOutOfBoundsException("negative value: index=" + index + " length=" + length);
		}
		
		final long resultingPosition = index + (long) length;
		
		if (resultingPosition > capacity) {
			if (resultingPosition > MAX_ARRAY_LENGTH) {
				throw new IndexOutOfBoundsException(
					"index=" + index  + " length=" + length + " maxCapacity=" + MAX_ARRAY_LENGTH);
			}
			final int newCapacity = calculateExpansion(capacity, resultingPosition);
			byteArray = Arrays.copyOf(byteArray, newCapacity);
			capacity = newCapacity;
		}
	}
	
	private static int calculateExpansion(final int currentLength, final long requiredLength) {
		long value = Math.max(currentLength, 2);
		while (value < requiredLength) {
			value = value + (value >> 1);
			if (value > MAX_ARRAY_LENGTH) {
				value = MAX_ARRAY_LENGTH;
			}
		}
		return (int)value;
	}
}
