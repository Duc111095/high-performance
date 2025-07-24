package com.ducnh.highperformance;

import static com.ducnh.highperformance.BitUtil.isPowerOfTwo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class BufferUtil {
	public static final byte[] NULL_BYTES = "null".getBytes(StandardCharsets.UTF_8);
	public static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();
	public static final long ARRAY_BASE_OFFSET = UnsafeApi.arrayBaseOffset(byte[].class);
	public static final long BYTE_BUFFER_HB_FIELD_OFFSET;
	public static final long BYTE_BUFFER_OFFSET_FIELD_OFFSET;
	public static final long BYTE_BUFFER_ADDRESS_FIELD_OFFSET;
	static 
	{
		try
		{
			BYTE_BUFFER_HB_FIELD_OFFSET = UnsafeApi.objectFieldOffset(
					ByteBuffer.class.getDeclaredField("hb"));
			BYTE_BUFFER_OFFSET_FIELD_OFFSET = UnsafeApi.objectFieldOffset(
					ByteBuffer.class.getDeclaredField("offset"));
			BYTE_BUFFER_ADDRESS_FIELD_OFFSET = UnsafeApi.objectFieldOffset(
					ByteBuffer.class.getDeclaredField("address"));
		}
		catch (final Exception ex) {
			throw new RuntimeException(ex);
		} 
	}
	private BufferUtil() {}
	
	/**
	 * Bounds check the access range and throw a {@link IndexOutOfBoundsException} if exceeded.
	 */
	public static void boundsCheck(final byte[] buffer, final long index, final int length) {
		final int capacity = buffer.length;
		final long resultingPosition = index + (long)length;
		if (index < 0 || resultingPosition > capacity) {
			throw new IndexOutOfBoundsException("index=" + index + " length=" + length + " capacity=" + capacity);			
		}
	} 
	
	/**
	 * Bounds check the access range and throw a {&link IndexOutOfBoundsException} if exceeded.
	 */
	public static void boundsCheck(final ByteBuffer buffer, final long index, final int length) {
		final int capacity = buffer.capacity();
		final long resultingPosition = index + (long)length;
		if (index < 0 || resultingPosition > capacity) {
			throw new IndexOutOfBoundsException("index=" + index + " length=" + length + " capacity=" + capacity);
		}
	}
	
	/**
	 * Get the address at which the underlying buffer storage begins.
	 */
	public static long address(final ByteBuffer buffer) {
		if (!buffer.isDirect()) {
			throw new IllegalArgumentException("buffer.isDirect() must be true");
		}
		return UnsafeApi.getLong(buffer, BYTE_BUFFER_ADDRESS_FIELD_OFFSET);
	}
	
	/**
	 * Get the array from a read-only {&link ByteBuffer} similar to {&link ByteBuffer#array()}.
	 */
	public static byte[] array(final ByteBuffer buffer) {
		if (buffer.isDirect()) {
			throw new IllegalArgumentException("buffer must wrap an array");
		}
		return (byte[])UnsafeApi.getReference(buffer, BYTE_BUFFER_HB_FIELD_OFFSET);
	}
	
	/**
	 * Get the array offset from a read-only {&link ByteBuffer} similar to {&link ByteBuffer#arrayOffset()}
	 */
	public static int arrayOffset(final ByteBuffer buffer) {
		return UnsafeApi.getInt(buffer, BYTE_BUFFER_OFFSET_FIELD_OFFSET);
	}
	
	/**
	 * Allocate a new direct {@link ByteBuffer} that is aligned on a given alignment boundary.
	 */
	public static ByteBuffer allocateDirectAligned(final int capacity, final int alignment) {
		if (!isPowerOfTwo(alignment)) {
			throw new IllegalArgumentException("Must be a power of 2: alignment=" + alignment);
		}
		final ByteBuffer buffer = ByteBuffer.allocateDirect(capacity + alignment);
		
		final long address = address(buffer);
		final int remainder =(int) (address & (alignment - 1));
		final int offset = alignment - remainder;
		
		buffer.limit(capacity + offset);
		buffer.position(offset);
		return buffer.slice();
	}
	
	/**
	 * Free the underlying direct {@link ByteBuffer} by invoking {@code Cleaner} on it. No op if {@code null} or 
	 * if the underlying {@link ByteBuffer} non-direct.
	 */
	public static void free(final DirectBuffer buffer) {
		if (null != buffer) {
			free(buffer.byteBuffer());
		}
	}
	
	/**
	 * Free the underlying direct {@link ByteBuffer} by invoking {@code Cleaner} on it. No op if {@code null} or
	 * non-direct {@link ByteBuffer}
	 */
	public static void free(final ByteBuffer buffer) {
		if (null !=  buffer && buffer.isDirect()) {
			UnsafeApi.invokeCleaner(buffer);
		}
	}
}
