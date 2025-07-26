package com.ducnh.highperformance;

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
	void put
}
