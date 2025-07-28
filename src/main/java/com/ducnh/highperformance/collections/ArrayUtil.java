package com.ducnh.highperformance.collections;

import java.lang.reflect.Array;
import java.util.Arrays;

public final class ArrayUtil {
	public static final int UNKNOWN_INDEX = -1;
	public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
	public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	public static final char[] EMPTY_CHAR_ARRAY = new char[0];
	public static final short[] EMPTY_SHORT_ARRAY = new short[0];
	public static final int[] EMPTY_INT_ARRAY = new int[0];
	public static final float[] EMPTY_FLOAT_ARRAY = new float[0];
	public static final long[] EMPTY_LONG_ARRAY = new long[0];
	public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
	public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
	public static final String[] EMPTY_STRING_ARRAY = new String[0];
	public static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;
	
	private ArrayUtil() {}
	
	public static <T> T[] add(final T[] oldElements, final T elementToAdd) {
		final int length = oldElements.length;
		final T[] newElements = Arrays.copyOf(oldElements, length + 1);
		newElements[length] = elementToAdd;
		return newElements;
	}
	
	public static <T> T[] remove(final T[] oldElements, final T elementToRemove) {
		final int length = oldElements.length;
		int index = UNKNOWN_INDEX;
		for (int  i = 0; i < length; i++) {
			if (oldElements[i] == elementToRemove) {
				index = i;
				break;
			}
		}
		return remove(oldElements, index);
	}
	
	public static <T> T[] remove(final T[] oldElements, final int index) {
		if (index == UNKNOWN_INDEX) {
			return oldElements;
		}
		
		final int oldLength = oldElements.length;
		final int newLength = oldLength - 1;
		final T[] newElements = newArray(oldElements, newLength);
		
		for (int i = 0, j = 0; i < oldLength; i ++) {
			if (index != i) {
				newElements[j++] = oldElements[i];
			}
		}
		
		return newElements;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] newArray(final T[] oldElements, final int length) {
		return (T[])Array.newInstance(oldElements.getClass().getComponentType(), length);
	}
	
	public static <T> T[] ensureCapacity(final T[] oldElements, final int requiredLength) {
		T[] result = oldElements;
		if (oldElements.length < requiredLength) {
			result = Arrays.copyOf(oldElements, requiredLength);
		}
		
		return result;
	}
}
