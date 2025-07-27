package com.ducnh.highperformance;

import java.nio.ByteBuffer;

import static com.ducnh.highperformance.BufferUtil.address;

public class ExpandableDirectByteBuffer extends AbstractMutableDirectBuffer{
	public static final int MAX_BUFFER_LENGTH = Integer.MAX_VALUE - 8;
	public static final int INITIAL_CAPACITY = 128;
	
	private ByteBuffer byteBuffer;
	
	public ExpandableDirectByteBuffer() {
		this(INITIAL_CAPACITY);
	}
	
	public ExpandableDirectByteBuffer(final int initialCapacity) {
		byteBuffer = ByteBuffer.allocateDirect(initialCapacity);
		addressOffset = address(byteBuffer);
		capacity = initialCapacity;
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
	
	public byte[] byteArray() {
		return null;
	}
	
	public ByteBuffer byteBuffer() {
		return byteBuffer;
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
		return "ExpandableDirectByteBuffer{" + 
				"address=" + addressOffset +
				", capacity=" + capacity + 
				", byteBuffer=" + byteBuffer +
				"}";
	}
	
	protected final void ensureCapacity(final int index, final int length) {
		if (index < 0 || length < 0) {
			throw new IndexOutOfBoundsException("negative value: index=" + index + " length=" + length);
		}
		
		final long resultingPosition = index + (long)length;
		final int currentCapacity = capacity;
		if (resultingPosition > currentCapacity) {
			if (resultingPosition > MAX_BUFFER_LENGTH) {
				throw new IndexOutOfBoundsException(
					"index=" + index + " length=" + length + " maxCapacity=" + MAX_BUFFER_LENGTH);
			}
			
			final int newCapacity = calculateExpansion(currentCapacity, resultingPosition);
			final ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);
			final long newAddress = address(newBuffer);
			
			getBytes(0, newBuffer, 0, currentCapacity);
			byteBuffer = newBuffer;
			addressOffset = newAddress;
			capacity = newCapacity;
		}
	}
	
	private static int calculateExpansion(final int currentLength, final long requiredLength) {
		long value = Math.max(currentLength, 2);
		
		while (value < requiredLength) {
			value = value + (value >> 1);
			if (value > MAX_BUFFER_LENGTH) {
				value = MAX_BUFFER_LENGTH;
			}
		}
		return (int)value;
	}
}
