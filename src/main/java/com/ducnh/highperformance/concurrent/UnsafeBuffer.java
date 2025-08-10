package com.ducnh.highperformance.concurrent;

import java.nio.ByteBuffer;

import com.ducnh.highperformance.AbstractMutableDirectBuffer;
import com.ducnh.highperformance.BufferUtil;
import com.ducnh.highperformance.DirectBuffer;
import com.ducnh.highperformance.UnsafeApi;

import static com.ducnh.highperformance.BitUtil.*;
import static com.ducnh.highperformance.BufferUtil.*;
import static com.ducnh.highperformance.collections.ArrayUtil.EMPTY_BYTE_ARRAY;

@SuppressWarnings("removal")
public class UnsafeBuffer extends AbstractMutableDirectBuffer implements AtomicBuffer{
	
	public static final int ALIGNMENT = AtomicBuffer.ALIGNMENT;
	public static final String DISABLE_BOUNDS_CHECK_PROP_NAME = DirectBuffer.DISABLE_BOUNDS_CHECKS_PROP_NAME;
	public static final boolean SHOULD_BOUNDS_CHECK = DirectBuffer.SHOULD_BOUNDS_CHECK;
	
	private ByteBuffer byteBuffer;
	private int wrapAdjustment;
	
	@SuppressWarnings("this-escape")
	public UnsafeBuffer() {
		wrap(EMPTY_BYTE_ARRAY);
	}
	
	@SuppressWarnings("this-escape")
	public UnsafeBuffer(final byte[] buffer) {
		wrap(buffer);
	}
	
	@SuppressWarnings("this-escape")
	public UnsafeBuffer(final ByteBuffer buffer) {
		wrap(buffer);
	}
	
	@SuppressWarnings("this-escape")
	public UnsafeBuffer(final ByteBuffer buffer, final int offset, final int length) {
		wrap(buffer, offset, length);
	}
	
	@SuppressWarnings("this-escape")
	public UnsafeBuffer(final DirectBuffer buffer) {
		wrap(buffer);
	}
	
	@SuppressWarnings("this-escape")
	public UnsafeBuffer(final DirectBuffer buffer, final int offset, final int length) {
		wrap(buffer, offset, length);
	}
	
	@SuppressWarnings("this-escape")
	public UnsafeBuffer(final long address, final int length) {
		wrap(address, length);
	}
	
	public void wrap(final byte[] buffer) {
		capacity = buffer.length;
		addressOffset = ARRAY_BASE_OFFSET;
		byteBuffer = null;
		wrapAdjustment = 0;
		
		if (buffer != byteArray) {
			byteArray = buffer;
		}
	}
	
	public void wrap(final byte[] buffer, final int offset, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheckWrap(offset, length, buffer.length);
		}
		
		capacity = length;
		addressOffset = ARRAY_BASE_OFFSET + offset;
		byteBuffer = null;
		wrapAdjustment = offset;
		
		if (buffer != byteArray) {
			byteArray = buffer;
		}
	}
	
	public void wrap(final ByteBuffer buffer) {
		capacity = buffer.capacity();
		
		if (buffer != byteBuffer) {
			byteBuffer = buffer;
		}
		
		if (buffer.isDirect()) {
			byteArray = null;
			addressOffset = address(buffer);
			wrapAdjustment = 0;
		}
		else {
			byteArray = BufferUtil.array(buffer);
			final int arrayOffset = arrayOffset(buffer);
			addressOffset = ARRAY_BASE_OFFSET + arrayOffset;
			wrapAdjustment = arrayOffset;
		}
	}
	
	public void wrap(final ByteBuffer buffer, final int offset, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheckWrap(offset, length, buffer.capacity());
		}
		
		capacity = length;
		
		if (buffer != byteBuffer) {
			byteBuffer = buffer;
		}
		
		if (buffer.isDirect()) {
			byteArray = null;
			addressOffset = address(buffer) + offset;
			wrapAdjustment = offset;
		} else {
			byteArray = BufferUtil.array(buffer);
			final int totalOffset = arrayOffset(buffer) + offset;
			addressOffset = ARRAY_BASE_OFFSET + totalOffset;
			wrapAdjustment = totalOffset;
		}
	}
	
	public void wrap(final DirectBuffer buffer) {
		capacity = buffer.capacity();
		addressOffset = buffer.addressOffset();
		wrapAdjustment = buffer.wrapAdjustment();
		
		final byte[] array = buffer.byteArray();
		if (array != this.byteArray) {
			this.byteArray = array;
		}
		
		final ByteBuffer byteBuffer = buffer.byteBuffer();
		if (byteBuffer != this.byteBuffer) {
			this.byteBuffer = byteBuffer;
		}
	}
	
	public void wrap(final DirectBuffer buffer, final int offset, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheckWrap(offset, length, buffer.capacity());
		}
		
		capacity = length;
		addressOffset = buffer.addressOffset() + offset;
		wrapAdjustment = buffer.wrapAdjustment() + offset;
		
		final byte[] array = buffer.byteArray();
		if (array != this.byteArray) {
			byteArray = array;
		}
		
		final ByteBuffer byteBuffer = buffer.byteBuffer();
		if (byteBuffer != this.byteBuffer) {
			this.byteBuffer = byteBuffer;
		}
	}
	
	public void wrap(final long address, final int length) {
		capacity = length;
		addressOffset = address;
		byteArray = null;
		byteBuffer = null;
	}
	
	public ByteBuffer byteBuffer() {
		return byteBuffer;
	}
	
	public int wrapAdjustment() {
		return wrapAdjustment;
	}
	
	public boolean isExpandable() {
		return false;
	}
	
	public void verifyAlignment() {
		if (null != byteArray) {
			final String msg = "AtomicBuffer was created from a byte[] and is not correctly aligned by " + ALIGNMENT;
			if (STRICT_ALIGNMENT_CHECKS) {
				throw new IllegalStateException(msg);
			} else {
				System.err.println(msg);
			}
		} else if (0 != (addressOffset & (ALIGNMENT - 1))) {
			throw new IllegalStateException(
				"AtomicBuffer is not correctly aligned: addressOffset=" + addressOffset + " is not divisible by " + 
				ALIGNMENT);
		} 
	}
	
	public long getLongVolatile(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck(index, SIZE_OF_LONG);
		}
		
		return UnsafeApi.getLongVolatile(byteArray, addressOffset + index);
	}
	
	public void putLongVolatile(final int index, final long value) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		UnsafeApi.putLongVolatile(byteArray,addressOffset + index, value);
	}

	public long getLongAcquire(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		return UnsafeApi.getLongAcquire(byteArray, addressOffset + index);
	}
	
	public void putLongOrdered(final int index, final long value) {
		putLongRelease(index, value);
	}
	
	public void putLongRelease(final int index, final long value) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		UnsafeApi.putLongRelease(byteArray, addressOffset + index, value);
	}
	
	public long addLongOrdered(final int index, final long increment) {
		return addLongRelease(index, increment);
	}
	
	public long addLongRelease(final int index, final long increment) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		return UnsafeApi.getAndAddLongRelease(byteArray,addressOffset + index, increment);
	}
	
	public void putLongOpaque(final int index, final long value) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		UnsafeApi.putLongOpaque(byteArray, addressOffset + index, value);
	}
	
	public long getLongOpaque(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		return UnsafeApi.getLongOpaque(byteArray,addressOffset + index);
	}
	
	public long addLongOpaque(final int index, final long increment) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		final long oldValue = UnsafeApi.getLongOpaque(byteArray, addressOffset + index);
		final long newValue = oldValue + increment;
		UnsafeApi.putLongOpaque(byteArray, addressOffset + index, newValue);
		return oldValue;
	}
	
	public boolean compareAndSetLong(final int index, final long expectedValue, final long updateValue) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		return UnsafeApi.compareAndSetLong(byteArray, addressOffset + index, expectedValue, updateValue);
	}
	
	public long compareAndExchangeLong(final int index, final long expectedValue, final long updateValue) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		return UnsafeApi.compareAndExchangeLong(byteArray, addressOffset + index, expectedValue, updateValue);
	}
	
	public long getAndSetLong(final int index, final long value) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		return UnsafeApi.getAndSetLong(byteArray, addressOffset + index, value);
	}
	
	public long getAndAddLong(final int index, final long delta) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		return UnsafeApi.getAndAddLong(byteArray, addressOffset + index, delta);
	}
	
	public int getIntVolatile(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_INT);
		}
		
		return UnsafeApi.getIntVolatile(byteArray, addressOffset + index);
	}
	
	public void putIntVolatile(final int index, final int value) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_INT);
		}
		
		UnsafeApi.putIntVolatile(byteArray, addressOffset + index, value);
	}
	
	public int getIntAcquire(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_INT);
		}
		
		return UnsafeApi.getIntAcquire(byteArray, addressOffset + index);
	}
	
	public void putIntOrdered(final int index, final int value) {
		putIntRelease(index, value);
	}
	
	public void putIntRelease(final int index, final int value) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_INT);
		}
		
		UnsafeApi.putIntRelease(byteArray,addressOffset + index, value);
	}
	
	public int addIntOrdered(final int index, final int increment) {
		return addIntRelease(index, increment);
	}
	
	public int addIntRelease(final int index, final int increment) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_INT);
		}
		
		return UnsafeApi.getAndAddIntRelease(byteArray, addressOffset + index, increment);
	}
	
	public void putIntOpaque(final int index, final int value) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_INT);
		}
		
		UnsafeApi.putIntOpaque(byteArray, addressOffset + index, value);
	}
	
	public int getIntOpaque(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_INT);
		}
		
		return UnsafeApi.getIntOpaque(byteArray, addressOffset + index);
	}
	
	public int addIntOpaque(final int index, final int increment) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_INT);
		}
		
		final int oldValue = UnsafeApi.getIntOpaque(byteArray, addressOffset + index);
		final int newValue = oldValue + increment;
		UnsafeApi.putIntOpaque(byteArray, addressOffset + index, newValue);
		return oldValue;
	}
	
	public boolean compareAndSetInt(final int index, final int expectedValue, final int updateValue) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_INT);
		}
		
		return UnsafeApi.compareAndSetInt(byteArray, addressOffset + index, expectedValue, updateValue);
	}
	
	public int compareAndExchangeInt(final int index, final int expectedValue, final int updateValue) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_INT);
		}
		
		return UnsafeApi.compareAndExchangeInt(byteArray, addressOffset + index, expectedValue, updateValue);
	}
	
	public int getAndSetInt(final int index, final int value) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck(index, SIZE_OF_INT);
		}
		
		return UnsafeApi.getAndSetInt(byteArray, addressOffset + index, value);
	}
	
	public int getAndAddInt(final int index, final int delta) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck(index, SIZE_OF_INT);
		}
		
		return UnsafeApi.getAndAddInt(byteArray, addressOffset + index, delta);
	}
	
	public short getShortVolatile(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_SHORT);
		}
		
		return UnsafeApi.getShortVolatile(byteArray, addressOffset + index);
	}
	
	public void putShortVolatile(final int index, final short value) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_SHORT);
		}
		
		UnsafeApi.putShortVolatile(byteArray, addressOffset + index, value);
	}
	
	public byte getByteVolatile(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_BYTE);
		}
		
		return UnsafeApi.getByteVolatile(byteArray, addressOffset + index);
	}
	
	public void putByteVolatile(final int index, final byte value) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_BYTE);
		}
		
		UnsafeApi.putByteVolatile(byteArray, addressOffset + index, value);
	}
	
	public char getCharVolatile(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		return UnsafeApi.getCharVolatile(byteArray, addressOffset + index);
	}
	
	public void putCharVolatile(final int index, final char value) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		UnsafeApi.putCharVolatile(byteArray, addressOffset + index, value);
	}
	
	public String toString() {
		return "UnsafeBuffer{" + 
			"addressOffset=" + addressOffset + 
			", capacity=" + capacity + 
			", byteArray=" + (null == byteArray ? "null" : ("byte[" + byteArray.length + "]")) +
			", byteBuffer=" + byteBuffer;
	}
	
	protected final void ensureCapacity(final int index, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, length);
		}
	}
	
	private static void boundsCheckWrap(final int offset, final int length, final int capacity) {
		if (offset < 0) {
			throw new IllegalArgumentException("invalid offset: " + offset);
		}
		
		if (length < 0) {
			throw new IllegalArgumentException("invalid length: " + length);
		}
		
		if ((offset > capacity - length) || (length > capacity - offset)) {
			throw new IllegalArgumentException(
					"offset=" + offset + " length=" + length + " not valid for capacity=" + capacity);
		}
	}
}
