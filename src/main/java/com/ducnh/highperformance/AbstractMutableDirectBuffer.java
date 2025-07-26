package com.ducnh.highperformance;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static com.ducnh.highperformance.BitUtil.*;
import static com.ducnh.highperformance.BufferUtil.*;
import static com.ducnh.highperformance.AsciiEncoding.*;

public abstract class AbstractMutableDirectBuffer implements MutableDirectBuffer{
	protected byte[] byteArray;
	protected long addressOffset;
	protected int capacity;
	
	protected AbstractMutableDirectBuffer() {}
	
	public byte[] byteArray() {
		return byteArray;
	}
	
	public long addressOffset() {
		return addressOffset;
	}
	
	public int capacity() {
		return capacity;
	}
	
	public void checkLimit(final int limit) {
		if (limit > capacity) {
			throw new IndexOutOfBoundsException("limit=" + limit + " is beyond capacity=" + capacity);
		}
	}
	
	public void setMemory(final int index, final int length, final byte value) {
		ensureCapacity(index, length);
		final byte[] array = byteArray;
		final long offset = addressOffset + index;
		
		if (length < 100) {
			int i = 0;
			final int end = (length & ~7);
			final long mask = ((((long)value) << 56) |
					(((long)value & 0xff) << 48) |
					(((long)value & 0xff) << 40) |
					(((long)value & 0xff) << 32) |
					(((long)value & 0xff) << 24) |
					(((long)value & 0xff) << 16) |
					(((long)value & 0xff) << 8) |
					(((long)value & 0xff)));
			for (; i < length; i += 8) {
				UnsafeApi.putLong(array, offset + i, mask);
			}
			
			for (; i < length; i++) {
				UnsafeApi.putByte(array, offset + i, value);
			}
		} else {
			UnsafeApi.setMemory(array, offset, length, value);
		}
	}
	
	public long getLong(final int index, final ByteOrder byteOrder) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_LONG);
		}
		
		long bits = UnsafeApi.getLong(byteArray, addressOffset + index);
		if (NATIVE_BYTE_ORDER != byteOrder) {
			bits = Long.reverseBytes(bits);
		}
		return bits;
	}
	
	public void putLong(final int index, final long value, final ByteOrder byteOrder) {
		ensureCapacity(index, SIZE_OF_LONG);
		long bits = value;
		if (NATIVE_BYTE_ORDER != byteOrder) {
			bits = Long.reverseBytes(bits);
		}
		UnsafeApi.putLong(byteArray, addressOffset + index, bits);
	}
	
	public long getLong(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck(index, SIZE_OF_LONG);
		}
		return UnsafeApi.getLong(byteArray, addressOffset + index);
	}
	
	public void putLong(final int index, final long value) {
		ensureCapacity(index, SIZE_OF_LONG);
		UnsafeApi.putLong(byteArray, addressOffset + index, value);
	}
	
	public int getInt(final int index, final ByteOrder byteOrder) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck(index, SIZE_OF_INT);
		}
		int bits = UnsafeApi.getInt(byteArray, addressOffset + index);
		if (NATIVE_BYTE_ORDER != byteOrder) {
			bits = Integer.reverseBytes(bits);
		}
		return bits;
	}
	
	public void putInt(final int index, final int value, final ByteOrder byteOrder) {
		ensureCapacity(index, SIZE_OF_INT);
		int bits = value;
		if (NATIVE_BYTE_ORDER != byteOrder) {
			bits = Integer.reverseBytes(bits);
		}
		UnsafeApi.putInt(byteArray, addressOffset + index, bits);
	}
	
	public int getInt(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_INT);
		}
		return UnsafeApi.getInt(byteArray, addressOffset + index);
	}
	
	public void putInt(final int index, final int value) {
		ensureCapacity(index, SIZE_OF_INT);
		UnsafeApi.putInt(byteArray, addressOffset + index, value);
	}
	
	public double getDouble(final int index, final ByteOrder byteOrder) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_DOUBLE);
		}
		
		if (NATIVE_BYTE_ORDER != byteOrder) {
			final long bits = UnsafeApi.getLong(byteArray, addressOffset + index);
			return Double.longBitsToDouble(Long.reverseBytes(bits));
		}
		else {
			return UnsafeApi.getDouble(byteArray, addressOffset + index);
		}
	}
	
	public void putDouble(final int index, final double value, final ByteOrder byteOrder) {
		ensureCapacity(index, SIZE_OF_DOUBLE);
		if (NATIVE_BYTE_ORDER != byteOrder) {
			final long bits = Long.reverseBytes(Double.doubleToRawLongBits(value));
			UnsafeApi.putLong(byteArray, addressOffset + index, bits);
		} else {
			UnsafeApi.putDouble(byteArray, addressOffset + index, value);
		}
	}
	
	public double getDouble(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_DOUBLE);
		}
		return UnsafeApi.getDouble(byteArray, addressOffset + index);
	}
	
	public void putDouble(final int index, final double value) {
		ensureCapacity(index, SIZE_OF_DOUBLE);
		UnsafeApi.putDouble(byteArray, addressOffset + index, value);
	}
	
	public float getFloat(final int index, final ByteOrder byteOrder) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_FLOAT);
		}
		if (NATIVE_BYTE_ORDER != byteOrder) {
			final int bits = UnsafeApi.getInt(byteArray, addressOffset + index);
			return Float.intBitsToFloat(Integer.reverseBytes(bits));
		} else {
			return UnsafeApi.getFloat(byteArray, addressOffset + index);
		}
	}
	
	public void putFloat(final int index, final float value, final ByteOrder byteOrder) {
		ensureCapacity(index, SIZE_OF_FLOAT);
		if (NATIVE_BYTE_ORDER != byteOrder) {
			final int bits = Integer.reverseBytes(Float.floatToRawIntBits(value));
			UnsafeApi.putInt(byteArray, addressOffset + index, bits);
		} else {
			UnsafeApi.putFloat(byteArray, addressOffset + index, value);
		}
	}
	
	public float getFloat(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_FLOAT);
		}
		return UnsafeApi.getFloat(byteArray, addressOffset + index);
	}
	
	public void putFloat(final int index, final float value) {
		ensureCapacity(index, SIZE_OF_FLOAT);
		UnsafeApi.putFloat(byteArray, addressOffset + index, value);
	}
	
	public short getShort(final int index, final ByteOrder byteOrder) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_SHORT);
		}
		
		short bits = UnsafeApi.getShort(byteArray, addressOffset + index);
		if (NATIVE_BYTE_ORDER != byteOrder) {
			bits = Short.reverseBytes(bits);
		}
		return bits;
	}
	
	public void putShort(final int index, final short value, final ByteOrder byteOrder) {
		ensureCapacity(index, SIZE_OF_SHORT);
		short bits = value;
		if (NATIVE_BYTE_ORDER != byteOrder) {
			bits = Short.reverseBytes(bits);
		}
		UnsafeApi.putShort(byteArray, addressOffset + index, bits);
	}
	
	public short getShort(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_SHORT);
		}
		return UnsafeApi.getShort(byteArray, addressOffset + index);
	}
	
	public void putShort(final int index, final short value) {
		ensureCapacity(index, SIZE_OF_SHORT);
		UnsafeApi.putShort(byteArray, addressOffset + index, value);
	}
	
	public byte getByte(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_BYTE);
		}
		
		return UnsafeApi.getByte(byteArray, addressOffset + index);
	}
	
	public void putByte(final int index, final byte value) {
		ensureCapacity(index, SIZE_OF_BYTE);
		UnsafeApi.putByte(byteArray, addressOffset + index,value);
	}
	
	public void getBytes(final int index, final byte[] dst) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, dst.length);
			BufferUtil.boundsCheck(dst, 0, dst.length);
		}
		UnsafeApi.copyMemory(byteArray, addressOffset + index, dst, ARRAY_BASE_OFFSET, dst.length);
	}
	  
	public void getBytes(final int index, final byte[] dst, final int offset, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, length);
			BufferUtil.boundsCheck(dst, offset, length);
		}
		UnsafeApi.copyMemory(byteArray, addressOffset + index, dst, ARRAY_BASE_OFFSET + offset, length);
	}
	
	public void getBytes(final int index, final MutableDirectBuffer dstBuffer, final int dstIndex, final int length) {
		dstBuffer.putBytes(dstIndex, this, index, length);
	}
	
	public void getBytes(final int index, final ByteBuffer dstBuffer, final int length) {
		final int dstOffset = dstBuffer.position();
		getBytes(index, dstBuffer, dstOffset, length);
		dstBuffer.position(dstOffset + length);
	}
	
	public void getBytes(final int index, final ByteBuffer dstBuffer, final int dstOffset, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, length);
			BufferUtil.boundsCheck(dstBuffer, dstOffset, length);
		}
		
		final byte[] dstByteArray;
		final long dstBaseOffset;
		if (dstBuffer.isDirect()) {
			dstByteArray = null;
			dstBaseOffset = address(dstBuffer);
		}
		else {
			dstByteArray = BufferUtil.array(dstBuffer);
			dstBaseOffset = ARRAY_BASE_OFFSET + arrayOffset(dstBuffer);
		}
		
		UnsafeApi.copyMemory(byteArray, addressOffset + index, dstByteArray, dstBaseOffset + dstOffset, length);
	}
	
	public void putBytes(final int index, final byte[] src) {
		ensureCapacity(index, src.length);
		UnsafeApi.copyMemory(src, ARRAY_BASE_OFFSET, byteArray, addressOffset + index, src.length);
	}
	
	public void putBytes(final int index, final byte[] src, final int offset, final int length) {
		ensureCapacity(index, length);
		if (SHOULD_BOUNDS_CHECK) {
			BufferUtil.boundsCheck(src, offset, length);
		}
		
		UnsafeApi.copyMemory(src, ARRAY_BASE_OFFSET + offset, byteArray, addressOffset + index, length);
	}
	
	public void putBytes(final int index, final ByteBuffer srcBuffer, final int length) {
		final int srcIndex = srcBuffer.position();
		putBytes(index, srcBuffer, srcIndex, length);
		srcBuffer.position(srcIndex + length);
	}
	
	public void putBytes(final int index, final ByteBuffer srcBuffer, final int srcIndex, final int length) {
		ensureCapacity(index, length);
		if (SHOULD_BOUNDS_CHECK) {
			BufferUtil.boundsCheck(srcBuffer, srcIndex, length);
		}
		
		final byte[] srcByteArray;
		final long srcBaseOffset;
		if (srcBuffer.isDirect()) {
			srcByteArray = null;
			srcBaseOffset = address(srcBuffer);
		} else {
			srcByteArray = BufferUtil.array(srcBuffer);
			srcBaseOffset = ARRAY_BASE_OFFSET + arrayOffset(srcBuffer);
		}
		
		UnsafeApi.copyMemory(srcByteArray, srcBaseOffset + srcIndex, byteArray, addressOffset + index, length);
	}
	
	public void putBytes(final int index, final DirectBuffer srcBuffer, final int srcIndex, final int length) {
		ensureCapacity(index, length);
		if (SHOULD_BOUNDS_CHECK) {
			srcBuffer.boundsCheck(srcIndex, length);
		}
		UnsafeApi.copyMemory(
				srcBuffer.byteArray(),
				srcBuffer.addressOffset() + srcIndex,
				byteArray,
				addressOffset + index,
				length);
	}
	
	public char getChar(final int index, final ByteOrder byteOrder) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_CHAR);
		}
		char bits = UnsafeApi.getChar(byteArray, addressOffset + index);
		if (NATIVE_BYTE_ORDER != byteOrder) {
			bits = (char) Short.reverseBytes((short)bits);
		}
		return bits;
	}
	
	public void putChar(final int index, final char value, final ByteOrder byteOrder) {
		ensureCapacity(index, SIZE_OF_CHAR);
		
		char bits = value;
		if (NATIVE_BYTE_ORDER != byteOrder) {
			bits = (char) Short.reverseBytes((short) bits);
		}
		UnsafeApi.putChar(byteArray, addressOffset + index, bits);
	}
	
	public char getChar(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, SIZE_OF_CHAR);
		}
		return UnsafeApi.getChar(byteArray, addressOffset + index);
	}
	
	public void putChar(final int index, final char value) {
		ensureCapacity(index, SIZE_OF_CHAR);
		UnsafeApi.putChar(byteArray, addressOffset + index, value);
	}
	
	public String getStringAscii(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, STR_HEADER_LEN);
		}
		final int length = UnsafeApi.getInt(byteArray, addressOffset + index);
		if (length == 0) {
			return "";
		}
		
		return getStringWithoutLengthAscii(index + STR_HEADER_LEN, length);
	}
	
	public String getStringAscii(final int index, final ByteOrder byteOrder) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, STR_HEADER_LEN);
		}
		
		int bits = UnsafeApi.getInt(byteArray, addressOffset + index);
		if (NATIVE_BYTE_ORDER != byteOrder) {
			bits = Integer.reverseBytes(bits);
		}
		
		final int length = bits;
		if (length == 0) {
			return "";
		}
		
		return getStringWithoutLengthAscii(index + STR_HEADER_LEN, length);
	}
	
	public int getStringAscii(final int index, final Appendable appendable, final ByteOrder byteOrder) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, STR_HEADER_LEN);
		}
		
		int bits = UnsafeApi.getInt(byteArray, addressOffset + index);
		if (byteOrder != NATIVE_BYTE_ORDER) {
			bits = Integer.reverseBytes(bits);
		}
		
		final int length = bits;
		if (length == 0) {
			return 0;
		}
		return getStringWithoutLengthAscii(index + STR_HEADER_LEN, length, appendable);
	}
	
	public String getStringAscii(final int index, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index + STR_HEADER_LEN, length);
		}
		
		if (length == 0) {
			return "";
		}
		
		final byte[] dst = new byte[length];
		UnsafeApi.copyMemory(byteArray, addressOffset + index + STR_HEADER_LEN, dst, ARRAY_BASE_OFFSET, length);
		
		return new String(dst, US_ASCII);
	}
	
	public int getStringAscii(final int index, final int length, final Appendable appendable) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, length + STR_HEADER_LEN);
		}
		
		try {
			final byte[] array = byteArray;
			final long offset = addressOffset;
			for (int i = index + STR_HEADER_LEN, limit = index + STR_HEADER_LEN + length; i < limit; i++) {
				final char c = (char)UnsafeApi.getByte(array, offset + i);
				appendable.append(c > 127 ? '?' : c);
			}
		} catch (final IOException ex) {
			LangUtil.rethrowUnchecked(ex);
		}
		return length;
	}
	
	public int putStringAscii(final int index, final String value) {
		if (value == null) {
			ensureCapacity(index, STR_HEADER_LEN);
			UnsafeApi.putInt(byteArray, addressOffset + index, 0);
			return STR_HEADER_LEN;
		} else {
			final int length = value.length();
			ensureCapacity(index, length + STR_HEADER_LEN);
			
			final byte[] array = byteArray;
			final long offset = addressOffset + index;
			UnsafeApi.putInt(array, offset, length);
			
			for (int i = 0; i < length; i++) {
				char c = value.charAt(i);
				if (c > 127) {
					c = '?';
				}
				UnsafeApi.putByte(array, offset + STR_HEADER_LEN + i, (byte)c);
			}
			return STR_HEADER_LEN + length;
		}
	}
	
	public int putStringAscii(final int index, final CharSequence value) {
		if (value == null) {
			ensureCapacity(index, STR_HEADER_LEN);
			UnsafeApi.putInt(byteArray, addressOffset + index, 0);
			return STR_HEADER_LEN;
		} else {
			final int length = value.length();
			ensureCapacity(index, length + STR_HEADER_LEN);
			
			final byte[] array = byteArray;
			final long offset = addressOffset + index;
			UnsafeApi.putInt(array, offset, length);
			
			for (int i = 0; i < length; i++) {
				char c = value.charAt(i);
				if (c > 127) {
					c = '?';
				}
				UnsafeApi.putByte(array, offset + STR_HEADER_LEN + i, (byte) c);
			}
			return STR_HEADER_LEN + length;
		}
	}
	
	public int putStringAscii(final int index, final String value, final ByteOrder byteOrder) {
		if (value == null) {
			ensureCapacity(index, STR_HEADER_LEN);
			UnsafeApi.putInt(byteArray, addressOffset + index, 0);
			return STR_HEADER_LEN;
		}
		else {
			final int length = value.length();
			ensureCapacity(index, length + STR_HEADER_LEN);
			
			int lengthBits = length;
			if (byteOrder != NATIVE_BYTE_ORDER) {
				lengthBits = Integer.reverseBytes(lengthBits);
			}
			
			final byte[] array = byteArray;
			final long offset = addressOffset + index;
			UnsafeApi.putInt(array, offset, lengthBits);
			
			for (int i = 0; i < length; i++) {
				char c = value.charAt(i);
				if (c > 127) {
					c = '?';
				}
				UnsafeApi.putByte(array, offset + STR_HEADER_LEN + i, (byte)c);
			}
			return STR_HEADER_LEN + length;
		}
	}
	
	public int putStringAscii(final int index, final CharSequence value, final ByteOrder byteOrder) {
		if (value == null) {
			ensureCapacity(index, STR_HEADER_LEN);
			UnsafeApi.putInt(byteArray, addressOffset + index, 0);
			return STR_HEADER_LEN;
		}
		else {
			final int length = value.length();
			ensureCapacity(index, length + STR_HEADER_LEN);
			
			int lengthBits = length;
			if (byteOrder != NATIVE_BYTE_ORDER) {
				lengthBits = Integer.reverseBytes(lengthBits);
			}
			
			final byte[] array = byteArray;
			final long offset = addressOffset + index;
			UnsafeApi.putInt(array, offset, lengthBits);
			
			for (int i = 0; i < length; i++) {
				char c = value.charAt(i);
				if (c > 127) {
					c = '?';
				}
				
				UnsafeApi.putByte(array, offset + STR_HEADER_LEN + i, (byte)c);
			}
			
			return STR_HEADER_LEN + length;
		}
	}
	public String getStringWithoutLengthAscii(final int index, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, length);
		}
		if (length == 0) {
			return "";
		}
		
		final byte[] dst = new byte[length];
		UnsafeApi.copyMemory(byteArray, addressOffset + index, dst, ARRAY_BASE_OFFSET, length);
		
		return new String(dst, US_ASCII);
	}
	
	public int getStringWithoutLengthAscii(final int index, final int length, final Appendable appendable) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, length);
		}
		
		try {
			final byte[] array = byteArray;
			final long offset = addressOffset;
			for (int i = index, limit = index + length; i < limit; i++) {
				final char c = (char) UnsafeApi.getByte(array, offset + i);
				appendable.append(c > 127 ? '?' : c);
			}
		} catch (final IOException ex) {
			LangUtil.rethrowUnchecked(ex);
		}
		return length;
	}
	
	public int putStringWithoutLengthAscii(final int index, final String value) {
		if (value == null) {
			return 0;
		}
		
		final int length = value.length();
		ensureCapacity(index, length);
		
		final byte[] array = byteArray;
		final long offset = addressOffset + index;
		for (int i = 0; i < length; i++) {
			char c = value.charAt(i);
			if (c > 127) {
				c = '?';
			}
			UnsafeApi.putByte(array, offset + i, (byte)c);
		}
		return length;
	}
	
	public int putStringWithoutLengthAscii(final int index, final CharSequence value) {
		if (value == null) {
			return 0;
		}
		
		final int length = value.length();
		
		ensureCapacity(index, length);
		
		final byte[] array = byteArray;
		final long offset = addressOffset + index;
		for (int i = 0; i < length; i++) {
			char c = value.charAt(i);
			if (c > 127) {
				c = '?';
			}
			UnsafeApi.putByte(array, offset + i, (byte)c);
		}
		return length;
	}
	
	public int putStringWithoutLengthAscii(final int index, final String value, final int valueOffset, final int length) {
		if (value == null) {
			return 0;
		}
		
		final int len = Math.min(value.length() - valueOffset, length);
		
		ensureCapacity(index, len);
		
		final byte[] array = byteArray;
		final long offset = addressOffset + index;
		for (int i = 0; i < len; i++) {
			char c = value.charAt(valueOffset + i);
			if (c > 127) {
				c = '?';
			}
			UnsafeApi.putByte(array, offset + i, (byte)c);
		}
		return len;
	}
	
	public int putStringWithoutLengthAscii(final int index, final CharSequence value, final int valueOffset, final int length) {
		if (value == null) {
			return 0;
		}
		
		final int len = Math.min(value.length() - valueOffset, length);
		
		ensureCapacity(index, len);
		
		final byte[] array = byteArray;
		final long offset = addressOffset + index;
		for (int i = 0; i < len; i++) {
			char c = value.charAt(valueOffset + i);
			if (c > 127) {
				c = '?';
			}
			UnsafeApi.putByte(array, offset + i, (byte)c);
		}
		return len;
	}
	
	public String getStringUtf8(final int index) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, STR_HEADER_LEN);
		}
		final int length = UnsafeApi.getInt(byteArray, addressOffset + index);
		if (length == 0) {
			return "";
		}
		
		return getStringWithoutLengthUtf8(index + STR_HEADER_LEN, length);
	}
	
	public String getStringUtf8(final int index, final ByteOrder byteOrder) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck(index, STR_HEADER_LEN);
		}
		
		int bits = UnsafeApi.getInt(byteArray, addressOffset + index);
		if (byteOrder != NATIVE_BYTE_ORDER) {
			bits = Integer.reverseBytes(bits);
		}
		
		final int length = bits;
		if (length == 0) {
			return "";
		}
		
		return getStringWithoutLengthUtf8(index + STR_HEADER_LEN, length);
	}
	
	public String getStringUtf8(final int index, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index + STR_HEADER_LEN, length);
		}
		
		final byte[] stringInBytes = new byte[length];
		
		UnsafeApi.copyMemory(byteArray, addressOffset + index + STR_HEADER_LEN, stringInBytes, ARRAY_BASE_OFFSET, length);
		
		return new String(stringInBytes, UTF_8);
	}
	
	public int putStringUtf8(final int index, final String value) {
		return putStringUtf8(index, value, Integer.MAX_VALUE);
	}
	
	public int putStringUtf8(final int index, final String value, final ByteOrder byteOrder) {
		return putStringUtf8(index, value, byteOrder, Integer.MAX_VALUE);
	}
	
	public int putStringUtf8(final int index, final String value, final int maxEncodedLength) {
		final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
		
		if (bytes.length > maxEncodedLength) {
			throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedLength);
		}
		
		ensureCapacity(index, STR_HEADER_LEN + bytes.length);
		
		final byte[] array = byteArray;
		final long offset = addressOffset + index;
		UnsafeApi.putInt(array, offset, bytes.length);
		UnsafeApi.copyMemory(bytes, ARRAY_BASE_OFFSET, array, offset + STR_HEADER_LEN, bytes.length);
		
		return STR_HEADER_LEN + bytes.length;
	}
	
	public int putStringUtf8(final int index, final String value, final ByteOrder byteOrder, final int maxEncodedLength) {
		final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
		if (bytes.length > maxEncodedLength) {
			throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedLength);
		}
		
		ensureCapacity(index, STR_HEADER_LEN + bytes.length);
		
		int bits = bytes.length;
		if (byteOrder != NATIVE_BYTE_ORDER) {
			bits = Integer.reverseBytes(bits);
		}
		
		final byte[] array = byteArray;
		final long offset = addressOffset + index;
		UnsafeApi.putInt(array, offset, bits);
		UnsafeApi.copyMemory(bytes, ARRAY_BASE_OFFSET, array, offset + STR_HEADER_LEN, bytes.length);
		
		return STR_HEADER_LEN + bytes.length;
	}
	
	public String getStringWithoutLengthUtf8(final int index, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, length);
		}
		
		if (length == 0) {
			return "";
		}
		
		final byte[] stringInBytes = new byte[length];
		UnsafeApi.copyMemory(byteArray,addressOffset + index, stringInBytes, ARRAY_BASE_OFFSET, length);
		return new String(stringInBytes, UTF_8);
	}
	
	public int putStringWithoutLengthUtf8(final int index, final String value) {
		final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
		ensureCapacity(index, bytes.length);
		
		UnsafeApi.copyMemory(bytes, ARRAY_BASE_OFFSET, byteArray, addressOffset + index, bytes.length);
		
		return bytes.length;
	}
	
	public int parseNaturalIntAscii(final int index, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, length);
		}
		
		if (length <= 0) {
			throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
		}
		
		if (length < INT_MAX_DIGITS) {
			return parsePositiveIntAscii(index, length, index, index + length);
		} else {
			final long tally = parsePositiveIntAsciiOverflowCheck(index, length, index, index + length);
			if (tally >= INTEGER_ABSOLUTE_MIN_VALUE) {
				throwParseIntOverflowError(index, length);
			}
			return (int) tally;
		}
	}
	
	public long parseNaturalLongAscii(final int index, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, length);
		}
		
		if (length <= 0) {
			throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
		}
		if (length < LONG_MAX_DIGITS) {
			return parsePositiveLongAscii(index, length, index, index + length);
		} else {
			return parseLongAsciiOverflowCheck(index, length, LONG_MAX_VALUE_DIGITS, index, index + length);
		}
	}
	
	public int parseIntAscii(final int index, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, length);
		}
		
		if (length <= 0) {
			throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
		}
		
		final boolean negative = MINUS_SIGN == UnsafeApi.getByte(byteArray, addressOffset + index);
		int i = index;
		if (negative) {
			i++;
			if (length == 1) {
				throwParseIntError(index, length);
			}
		}
		
		final int end = index + length;
		if (end - i < INT_MAX_DIGITS) {
			final int tally = parsePositiveIntAscii(index, length, i, end);
			return negative ? -tally : tally;
		} else {
			final long tally = parsePositiveIntAsciiOverflowCheck(index, length, i, end);
			if (tally > INTEGER_ABSOLUTE_MIN_VALUE || INTEGER_ABSOLUTE_MIN_VALUE == tally && !negative) {
				throwParseIntOverflowError(index, length);
			}
			return (int)(negative ? -tally : tally);
		}
	}
	
	public long parseLongAscii(final int index, final int length) {
		if (SHOULD_BOUNDS_CHECK) {
			boundsCheck0(index, length);
		}
		if (length <= 0) {
			throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
		}
		
		final boolean negative = MINUS_SIGN == UnsafeApi.getByte(byteArray, addressOffset + index);
		int i = index;
		if (negative) {
			i++;
			if (length == 1) {
				throwParseLongError(index, length);
			}
		}
		final int end = index + length;
		if (end - i < LONG_MAX_DIGITS) {
			final long tally = parsePositiveLongAscii(index, length, i, end);
			return negative ? -tally : tally;
		}
		else if (negative) {
			return -parseLongAsciiOverflowCheck(index, length, LONG_MIN_VALUE_DIGITS, i, end);
		}
		else {
			return parseLongAsciiOverflowCheck(index, length, LONG_MAX_VALUE_DIGITS, i, end);
		}
	}
	
	public int putIntAscii(final int index, final int value) {
		if (value == 0) {
			putByte(index, ZERO);
			return 1;
		}
		final byte[] array;
		long offset;
		int quotient = value;
		final int digitCount, length;
		if (value < 0) {
			if (Integer.MIN_VALUE == value) {
				putBytes(index, MIN_INTEGER_VALUE);
				return MIN_INTEGER_VALUE.length;
			}
			
			quotient = -quotient;
			digitCount = digitCount(quotient);
			length = digitCount + 1;
			
			ensureCapacity(index, length);
			array = byteArray;
			offset = addressOffset + index;
			
			UnsafeApi.putByte(array, offset, MINUS_SIGN);
			offset ++;
		} else {
			length = digitCount = digitCount(quotient);
			
			ensureCapacity(index, length);
			array = byteArray;
			offset = addressOffset + index;
		}
		
		putPositiveIntAscii(array, offset, quotient, digitCount);
	
		return length;
	}
	
	public int putNaturalIntAscii(final int index, final int value) {
		if (value == 0) {
			putByte(index, ZERO);
			return 1;
		}
		final int digitCount = digitCount(value);
		ensureCapacity(index, digitCount);
		putPositiveIntAscii(byteArray, addressOffset + index, value, digitCount);
		
		return digitCount;
	}
	
	public void putNaturalPaddedIntAscii(final int offset, final int length, final int value) {
		ensureCapacity(offset, length);
		
		final byte[] array = byteArray;
		final long addressOffset = this.addressOffset;
		final int end = offset + length;
		int remainder = value;
		for (int index = end - 1; index >= offset; index --) {
			final int digit = remainder % 10;
			remainder = remainder / 10;
			UnsafeApi.putByte(array, addressOffset + index, (byte)(ZERO + digit));
		}
		
		if (remainder != 0) {
			throw new NumberFormatException("Cannot write " + value + " in " + length + " bytes");
		}
	}
	
	public int putNaturalIntAsciiFromEnd(final int value, final int endExclusive) {
		final int length = digitCount(value);
		ensureCapacity(endExclusive - length, length);
		
		final byte[] array = byteArray;
		final long addressOffset = this.addressOffset;
		int remainder = value;
		int index = endExclusive;
		while (remainder > 0) {
			index --;
			final int digit = remainder % 10;
			remainder = remainder / 10;
			UnsafeApi.putByte(array, addressOffset + index, (byte)(ZERO + digit));
		}
		
		return index;
	}
	
	public int putNaturalLongAscii(final int index, final long value) {
		if (value == 0L) {
			putByte(index, ZERO);
			return 1;
		}
		
		final int digitCount = digitCount(value);
		
		ensureCapacity(index, digitCount);
		putPositiveLongAscii(byteArray, addressOffset + index, value, digitCount);
		
		return digitCount;
	}
	
	public int putLongAscii(final int index, final long value) {
		if (value == 0L) {
			putByte(index, ZERO);
			return 1;
		}
		
		final byte[] array;
		long offset;
		long quotient = value;
		final int digitCount, length;
		
		if (value < 0) {
			if (value == Long.MIN_VALUE) {
				putBytes(index, MIN_LONG_VALUE);
				return MIN_LONG_VALUE.length;
			}
			
			quotient = -quotient;
			digitCount = digitCount(quotient);
			length = digitCount + 1;
			
			ensureCapacity(index, length);
			array = byteArray;
			offset = addressOffset + index;
			
			UnsafeApi.putByte(array, offset, MINUS_SIGN);
			offset++;
		} else {
			length = digitCount = digitCount(quotient);
			
			ensureCapacity(index, length);
			array = byteArray;
			offset = addressOffset + index;
		}
		
		putPositiveLongAscii(array, offset, quotient, digitCount);
		
		return length;
	}
	
	public void boundsCheck(final int index, final int length) {
		boundsCheck0(index, length);
	}
	
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		
		final AbstractMutableDirectBuffer that = (AbstractMutableDirectBuffer) obj;
		
		final int length = capacity;
		
		if (length != that.capacity) {
			return false;
		}
		
		final byte[] thisArray = this.byteArray;
		final byte[] thatArray = that.byteArray;
		final long thisOffset = this.addressOffset;
		final long thatOffset = that.addressOffset;
		
		int i = 0;
		for (int end = length & ~7; i < end; i += 8) {
			if (UnsafeApi.getLong(thisArray, thisOffset + i) != UnsafeApi.getLong(thatArray, thatOffset + i)) {
				return false;
			}
		}
		
		for (; i < length; i++) {
			if (UnsafeApi.getByte(thisArray, thisOffset + i) != UnsafeApi.getByte(thatArray, thatOffset + i)) {
				return false;
			}
		}
		
		return true;
	}
	
	public int hashCode() {
		final byte[] array = byteArray;
		final long addressOffset = this.addressOffset;
		final int length = capacity;
		int i = 0, hashCode = 19;
		for (int end = length & ~7; i < end; i += 8) {
			hashCode = 31 * hashCode + Long.hashCode(UnsafeApi.getLong(array, addressOffset + i));
		}
		
		for (; i < length; i++) {
			hashCode = 31 * hashCode + UnsafeApi.getByte(array, addressOffset + i);
		}
		
		return hashCode;
	}
	
	public int compareTo(final DirectBuffer that) {
		if (this == that) {
			return 0;
		}
		
		final int thisCapacity = this.capacity;
		final int thatCapacity = that.capacity();
		final byte[] thisArray = this.byteArray;
		final byte[] thatArray = that.byteArray();
		final long thisOffset = this.addressOffset;
		final long thatOffset = that.addressOffset();
		final int length = Math.min(thisCapacity, thatCapacity);
		
		int i = 0;
		
		for (int end = length & ~7; i < end; i += 8) {
			final int cmp = Long.compare(
					UnsafeApi.getLong(thisArray, thisOffset + i),
					UnsafeApi.getLong(thatArray, thatOffset + i));
			if (0 != cmp) {
				return cmp;
			}
		}
		
		for (; i < length; i++) {
			final int cmp = Byte.compare(
					UnsafeApi.getByte(thisArray, thisOffset + i),
					UnsafeApi.getByte(thatArray, thatOffset + i));
			if (cmp != 0) {
				return cmp;
			}
		}
		
		return Integer.compare(thisCapacity, thatCapacity);
	}
	
	protected final void boundsCheck0(final int index, final int length) {
		final long resultingPosition = index + (long)length;
		if (index < 0 || length < 0 || resultingPosition > capacity) {
			throw new IndexOutOfBoundsException("index=" + index + " length=" + length + " capacity=" + capacity);
		}
	}

	protected abstract void ensureCapacity(int index, int length);
	
	private int parsePositiveIntAscii(final int index, final int length, final int startIndex, final int end) {
		final long offset = addressOffset;
		final byte[] array = byteArray;
		int i = startIndex;
		int tally = 0, quartet;
		while ((end - i) >= 4 && isFourDigitAsciiEncodedNumber(quartet = UnsafeApi.getInt(array, offset + 1))) {
			if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN) {
				quartet = Integer.reverseBytes(quartet);
			}
			
			tally = (tally * 10_000) + parseFourDigitsLittleEndian(quartet);
			i += 4;
		}
		
		byte digit;
		while (i < end && isDigit(digit = UnsafeApi.getByte(array, offset + i))) {
			tally = (tally * 10) + (digit - 0x30);
			i++;
		}
		
		if (i != end) {
			throwParseIntError(index, length);
		}
		
		return tally;
	}
	
	private long parsePositiveIntAsciiOverflowCheck(
			final int index, final int length, final int startIndex, final int end) {
		if ((end - startIndex) > INT_MAX_DIGITS) {
			throwParseIntOverflowError(index, length);
		}
		
		final long offset = addressOffset;
		final byte[] array = byteArray;
		int i = startIndex;
		long tally = 0;
		long octet = UnsafeApi.getLong(array, offset + i);
		
		if (isEightDigitAsciiEncodedNumber(octet)) {
			if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN) {
				octet = Long.reverseBytes(octet);
			}
			
			tally = parseEightDigitsLittleEndian(octet);
			i += 8;
			
			byte digit;
			while (i < end && isDigit(digit = UnsafeApi.getByte(array, offset + i))) {
				tally = (tally * 10L) + (digit - 0x30);
				i++;
			}
		}
		
		if (i != end) {
			throwParseIntError(index, length);
		}
		
		return tally;
	}
	
	private void throwParseIntError(final int index, final int length) {
		throw new AsciiNumberFormatException("error parsing int: " + getStringWithoutLengthAscii(index, length));
	}
	
	private void throwParseIntOverflowError(final int index, final int length) {
		throw new AsciiNumberFormatException("int overflow parsing: " + getStringWithoutLengthAscii(index, length));
	}
	
	private long parsePositiveLongAscii(final int index, final int length, final int startIndex, final int end) {
		final long offset = addressOffset;
		final byte[] array = byteArray;
		int i = startIndex;
		long tally = 0, octet;
		while ((end - i) >= 8 && isEightDigitAsciiEncodedNumber(octet = UnsafeApi.getLong(array, offset + i))) {
			if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN) {
				octet = Long.reverseBytes(octet);
			}
			
			tally = (tally * 100_000_000L) + parseEightDigitsLittleEndian(octet);
			i += 8;
		}
		
		int quartet;
		while ((end - i) >= 4 && isFourDigitAsciiEncodedNumber(quartet = UnsafeApi.getInt(array, offset + i))) {
			if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN) {
				quartet = Integer.reverseBytes(quartet);
			}
			
			tally = (tally * 10_000L) + parseFourDigitsLittleEndian(quartet);
			i += 4;
		}
		
		byte digit;
		while (i < end && isDigit(digit = UnsafeApi.getByte(array, offset + i))) {
			tally = (tally * 10) + (digit - 0x30);
			i++;
		}
		
		if (i != end) {
			throwParseLongError(index, length);
		}
		
		return tally;
	}
	
	private long parseLongAsciiOverflowCheck(
			final int index,
			final int length,
			final int[] maxValue,
			final int startIndex,
			final int end) {
		if ((end - startIndex) > LONG_MAX_DIGITS) {
			throwParseLongOverflowError(index, length);
		}
		
		final long offset = addressOffset;
		final byte[] array = byteArray;
		int i = startIndex, k = 0;
		boolean checkOverflow = true;
		long tally = 0, octet;
		while ((end - i) >= 8 && isEightDigitAsciiEncodedNumber(octet = UnsafeApi.getLong(array, offset + i))) {
			if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN) {
				octet = Long.reverseBytes(octet);
			}
			
			final int eightDigits = parseEightDigitsLittleEndian(octet);
			if (checkOverflow) {
				if (eightDigits > maxValue[k]) {
					throwParseLongOverflowError(index, length);
				} else if (eightDigits < maxValue[k]) {
					checkOverflow = false;
				}
				k++;
			}
			tally = (tally * 100_000_000L) + eightDigits;
			i += 8;
		}
		
		byte digit;
		int lastDigits = 0;
		while (i < end && isDigit(digit = UnsafeApi.getByte(array, offset + i))) {
			lastDigits = (lastDigits * 10) + (digit - 0x30);
			i++;
		}
		
		if (i != end) {
			throwParseLongError(index, length);
		}
		else if (checkOverflow && lastDigits > maxValue[k]) {
			throwParseLongOverflowError(index, length);
		}
		
		return (tally * 1000L) + lastDigits;
	}
	
	private void throwParseLongError(final int index, final int length) {
		throw new AsciiNumberFormatException("error parsing long: " + getStringWithoutLengthAscii(index, length));
	}
	
	private void throwParseLongOverflowError(final int index, final int length) {
		throw new AsciiNumberFormatException("long overflow parsing: " + getStringWithoutLengthAscii(index, length));
	}
	
	private static void putPositiveIntAscii(final byte[] dest, final long offset, final int value, final int digitCount) {
		int i = digitCount;
		int quotient = value;
		while (quotient >= 10_000) {
			final int lastFourDigits = quotient % 10_000;
			quotient /= 10_000;
			
			final int p1 = (lastFourDigits / 100) << 1;
			final int p2 = (lastFourDigits % 100) << 1;
			i -= 4;
			
			UnsafeApi.putByte(dest, offset + i, ASCII_DIGITS[p1]);
			UnsafeApi.putByte(dest, offset + i + 1, ASCII_DIGITS[p1 + 1]);
			UnsafeApi.putByte(dest, offset + i + 2, ASCII_DIGITS[p2]);
			UnsafeApi.putByte(dest, offset + i + 3, ASCII_DIGITS[p2 + 1]);
		}
		
		if (quotient >= 100) {
			final int position = (quotient % 100) << 1;
			quotient /= 100;
			UnsafeApi.putByte(dest, offset + i - 1, ASCII_DIGITS[position + 1]);
			UnsafeApi.putByte(dest, offset + i - 2, ASCII_DIGITS[position]);
		}
		
		if (quotient >= 10) {
			final int position = quotient << 1;
			UnsafeApi.putByte(dest, offset + 1, ASCII_DIGITS[position + 1]);
			UnsafeApi.putByte(dest, offset,  ASCII_DIGITS[position]);
		} else {
			UnsafeApi.putByte(dest, offset, (byte)(ZERO + quotient));
		}
	}
	
	public static void putPositiveLongAscii(
			final byte[] dest, final long offset, final long value, final int digitCount) {
		long quotient = value;
		int i = digitCount;
		while (quotient >= 100_000_000) {
			final int lastEightDigits = (int)(quotient % 100_000_000);
			quotient /= 100_000_000;
			
			final int upperPart = lastEightDigits  / 10_000;
			final int lowerPart = lastEightDigits % 10_000;
			
			final int u1 = (upperPart / 100) << 1;
			final int u2 = (upperPart % 100) << 1;
			final int l1 = (lowerPart / 100) << 1;
			final int l2 = (lowerPart % 100) << 1;
			
			i -= 8;
			
			UnsafeApi.putByte(dest, offset + i, ASCII_DIGITS[u1]);
			UnsafeApi.putByte(dest, offset + i + 1, ASCII_DIGITS[u1 + 1]);
			UnsafeApi.putByte(dest, offset + i + 2, ASCII_DIGITS[u2]);
			UnsafeApi.putByte(dest, offset + i + 3, ASCII_DIGITS[u2 + 1]);
			UnsafeApi.putByte(dest, offset + i + 4, ASCII_DIGITS[l1]);
			UnsafeApi.putByte(dest, offset + i + 5, ASCII_DIGITS[l1 + 1]);
			UnsafeApi.putByte(dest, offset + i + 6, ASCII_DIGITS[l2]);
			UnsafeApi.putByte(dest, offset + i + 7, ASCII_DIGITS[l2 + 1]);
		}
		
		putPositiveIntAscii(dest, offset, (int) quotient, i);
	}
}
