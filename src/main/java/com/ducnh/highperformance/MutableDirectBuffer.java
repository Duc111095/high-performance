package com.ducnh.highperformance;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface MutableDirectBuffer extends DirectBuffer{
	boolean isExpandable();
	void setMemory(int index, int length, byte value);
	void putLong(int index, long value, ByteOrder byteOrder);
	void putLong(int index, long value);
	void putInt(int index, int value, ByteOrder byteOrder);
	void putInt(int index, int value);
	int putIntAscii(int index, int value);
	int putNaturalIntAscii(int index, int value);
	void putNaturalPaddedIntAscii(int index, int length, int value) throws NumberFormatException;
	int putNaturalIntAsciiFromEnd(int value, int endExclusive);
	int putNaturalLongAscii(int index, long value);
	int putLongAscii(int index, long value);
	void putDouble(int index, double value, ByteOrder byteOrder);
	void putDouble(int index, double value);
	void putFloat(int index, float value, ByteOrder byteOrder);
	void putFloat(int index, float value);
	void putShort(int index, short value, ByteOrder byteOrder);
	void putShort(int index, short value);
	void putChar(int index, char value, ByteOrder byteOrder);
	void putChar(int index, char value);
	void putByte(int index, byte value);
	void putBytes(int index, byte[] src);
	void putBytes(int index, byte[] src, int offset, int length);
	void putBytes(int index, ByteBuffer srcBuffer, int length);
	void putBytes(int index, ByteBuffer srcBuffer, int srcIndex, int length);
	void putBytes(int index, DirectBuffer srcBuffer, int srcIndex, int length);
	int putStringAscii(int index, String value);
	int putStringAscii(int index, CharSequence value);
	int putStringAscii(int index, String value, ByteOrder byteOrder);
	int putStringAscii(int index, CharSequence value, ByteOrder byteOrder);
	int putStringWithoutLengthAscii(int index, String value);
	int putStringWithoutLengthAscii(int index, CharSequence value);
	int putStringWithoutLengthAscii(int index, String value, int valueOffset, int length);
	int putStringWithoutLengthAscii(int index, CharSequence value, int valueOffset, int length);
	int putStringUtf8(int index, String value);
	int putStringUtf8(int index, String value, ByteOrder byteOrder);
	int putStringUtf8(int index, String value, int maxEncodedLength);
	int putStringUtf8(int index, String value, ByteOrder byteOrder, int maxEncodedLength);
	int putStringWithoutLengthUtf8(int index, String value);
}
