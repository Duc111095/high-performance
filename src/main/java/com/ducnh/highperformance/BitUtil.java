package com.ducnh.highperformance;

import static java.nio.charset.StandardCharsets.US_ASCII;
import java.util.concurrent.ThreadLocalRandom;

public class BitUtil {
	public static final int SIZE_OF_BYTE = 1;
	public static final int SIZE_OF_BOOLEAN = 1;
	public static final int SIZE_OF_CHAR = 2;
	public static final int SIZE_OF_SHORT = 2;
	public static final int SIZE_OF_INT = 4;
	public static final int SIZE_OF_FLOAT = 4;
	public static final int SIZE_OF_DOUBLE = 8;
	public static final int SIZE_OF_LONG = 8;
	
	/**
	 * Length of the data blocks used by the CPU cache sub-system in bytes.
	 */
	public static final int CACHE_LINE_LENGTH = 64;
	private static final byte[] HEX_DIGIT_TABLE = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};
	private static final byte[] FROM_HEX_DIGIT_TABLE;
	
	static {
		FROM_HEX_DIGIT_TABLE = new byte[128];
		
		FROM_HEX_DIGIT_TABLE['0'] = 0x00;
		FROM_HEX_DIGIT_TABLE['1'] = 0x01;
		FROM_HEX_DIGIT_TABLE['2'] = 0x02;
		FROM_HEX_DIGIT_TABLE['3'] = 0x03;
		FROM_HEX_DIGIT_TABLE['4'] = 0x04;
		FROM_HEX_DIGIT_TABLE['5'] = 0x05;
		FROM_HEX_DIGIT_TABLE['6'] = 0x06;
		FROM_HEX_DIGIT_TABLE['7'] = 0x07;
		FROM_HEX_DIGIT_TABLE['8'] = 0x08;
		FROM_HEX_DIGIT_TABLE['9'] = 0x09;
		FROM_HEX_DIGIT_TABLE['a'] = 0x0a;
		FROM_HEX_DIGIT_TABLE['A'] = 0x0a;
		FROM_HEX_DIGIT_TABLE['b'] = 0x0b;
		FROM_HEX_DIGIT_TABLE['B'] = 0x0b;
		FROM_HEX_DIGIT_TABLE['c'] = 0x0c;
		FROM_HEX_DIGIT_TABLE['C'] = 0x0c;
		FROM_HEX_DIGIT_TABLE['d'] = 0x0d;
		FROM_HEX_DIGIT_TABLE['D'] = 0x0d;
		FROM_HEX_DIGIT_TABLE['e'] = 0x0e;
		FROM_HEX_DIGIT_TABLE['E'] = 0x0e;
		FROM_HEX_DIGIT_TABLE['f'] = 0x0f;
		FROM_HEX_DIGIT_TABLE['F'] = 0x0f;                     
	}
	
	private static final int LAST_DIGIT_MASK = 0b1;
	private BitUtil() {}
	
	/**
	 * Fast method of finding the next power of 2 greater than or equal to the supplied value.
	 * NumberOfLeadingZeros: the number of zero bits preceding the highest order of one-bit (leftmost) in binary
	 * representation of i value.
	 */
	public static int findNextPositivePowerOfTwo(final int value) {
		return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
	}
	
	public static long findNextPositivePowerOfTwo(final long value) {
		return 1L << (Long.SIZE - Long.numberOfLeadingZeros(value - 1));
	}
	
	
	/**
	 * Align a value to the next multiple up of alignment.
	 * If the value equals an alignment multiple then it is returned unchanged.
	 */
	public static int align(final int value, final int alignment) {
		return (value + (alignment - 1)) & alignment;
	}
	
	public static long align(final long value, final long alignment) {
		return (value + (alignment - 1)) & alignment;
	}
	
	/**
	 * Generate a byte array from the hex representation of the given byte array.
	 * @param buffer to convert from a hex representation (in Big Endian)
	 * @return new byte array that is decimal representation of the passed array.
	 */
	public static byte[] fromHexByteArray(final byte[] buffer) {
		final byte[] outputBuffer = new byte[buffer.length >> 1];
		
		for (int i = 1; i < buffer.length; i += 2) {
			final int hi = FROM_HEX_DIGIT_TABLE[buffer[i-1]] << 4;
			final int lo = FROM_HEX_DIGIT_TABLE[buffer[i]];
			outputBuffer[(i-1) >> 1] = (byte) (hi | lo);
		}
		
		return outputBuffer;
	}
	
	/**
	 * Generate a byte array that is a hex representation of a given byte array.
	 */
	public static byte[] toHexByteArray(final byte[] buffer) {
		return toHexByteArray(buffer, 0, buffer.length);
	}
	
	public static byte[] toHexByteArray(final byte[] buffer, final int offset, final int length) {
		final byte[] outputBuffer = new byte[length << 1];
		for (int i = 0; i < (length << 1); i += 2) {
			final byte b = buffer[offset + (i >> 1)];
			outputBuffer[i] = HEX_DIGIT_TABLE[(b >> 4) & 0x0F];
			outputBuffer[i + 1] = HEX_DIGIT_TABLE[b & 0x0F];
		}
		return outputBuffer;
	}
	
	public static byte[] toHexByteArray(final CharSequence charSequence, final int offset, final int length) {
		final byte[] outputBuffer = new byte[length << 1];
		for (int i = 0; i < (length << 1); i += 2) {
			final byte b = (byte)charSequence.charAt(offset + (i >> 1));
			outputBuffer[i] = HEX_DIGIT_TABLE[(b >> 4) & 0x0F];
			outputBuffer[i + 1] = HEX_DIGIT_TABLE[b & 0x0F];
		}
		return outputBuffer;
	}
	
	/**
	 * Generate a byte array from a string that is the hex representation of the given byte array.
	 */
	public static byte[] fromHex(final String string) {
		final int length = string.length();
		final byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			bytes[i] = (byte)string.charAt(i);
		}
		return fromHexByteArray(bytes);
	}
	
	/**
	 * Generate a string that is the hex representation of a given byte array.
	 */
	public static String toHex(final byte[] buffer, final int offset, final int length) {
		return new String(toHexByteArray(buffer, offset, length), US_ASCII);
	}
	
	public static String toHex(final byte[] buffer) {
		return new String(toHexByteArray(buffer), US_ASCII);
	}
	
	/**
	 * Is a int value even.
	 */
	public static boolean isEven(final int value) {
		return (value & LAST_DIGIT_MASK) == 0;
	}
	
	/**
	 * Is a long value even.
	 */
	public static boolean isEven(final long value) {
		return (value & LAST_DIGIT_MASK) == 0;
	}
	
	/**
	 * Is a value a positive power of 2.
	 */
	public static boolean isPowerOfTwo(final int value) {
		return value > 0 && ((value & (~value + 1)) == value);
	}
	
	public static boolean isPowerOfTwo(final long value) {
		return value > 0 && ((value & (~value + 1)) == value);
	}
	
	/**
	 * Cycles indicies of an array one at a time in a forward fashion.
	 */
	public static int next(final int current, final int max) {
		int next = current + 1;
		if (next == max) {
			next = 0;
		} 
		return next;
	}
	
	/**
	 * Cycles indicies of an array one at a time in a backwards fashion.
	 */
	public static int previous(final int current, final int max) {
		int previous = current - 1;
		if (previous < 0) {
			previous = max - 1;
		}
		return previous;
	}
	
	/**
	 * Calculate the shift value to scale a number based on how refs are compressed or not.
	 */
	public static int calculateShiftForScale(final int scale) {
		if (scale == 4) {
			return 2;
		} else if (scale == 8) {
			return 3;
		} else {
			throw new IllegalArgumentException("unknown pointer size for scale = " + scale);
		}
	}
	
	/**
	 * Generate a randomized integer over [{@link Integer#MIN_VALUE}, {@link Integer#MAX_VALUE}].
	 */
	public static int generateRandomisedId() {
		return ThreadLocalRandom.current().nextInt();
	}
	
	/**
	 * Is an address aligned on a boundary.
	 */
	public static boolean isAligned(final long address, final int alignment) {
		if (!BitUtil.isPowerOfTwo(alignment)) {
			throw new IllegalArgumentException("alignment must be a power of 2: alignment = " + alignment);
		} 
		return (address & (alignment - 1)) == 0;
	}
}
