package com.ducnh.highperformance;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_INT;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public interface DirectBuffer {
	/**
	 * Length of the header of strings on denote the length of the string in bytes.
	 */
	int STR_HEADER_LEN = SIZE_OF_INT;
	String DISABLE_ARRAY_CONTENT_PRINTOUT_PROP_NAME = "com.ducnh.array.printout";
	String DISABLE_BOUNDS_CHECKS_PROP_NAME = "com.ducnh.bounds.checks";
	boolean SHOULD_BOUNDS_CHECK = !"true".equals(DISABLE_ARRAY_CONTENT_PRINTOUT_PROP_NAME);
	void wrap(byte[] buffer);
	void wrap(byte[] buffer, int offset, int length);
	void wrap(ByteBuffer buffer);
	void wrap(ByteBuffer buffer, int offset, int length);
	void wrap(DirectBuffer buffer);
	void wrap(DirectBuffer buffer, int offset, int length);
	void wrap(long address, int length);
	long addressOffset();
	byte[] byteArray();
	ByteBuffer byteBuffer();
	int capacity();
	void checkLimit(int limit);
	long getLong(int index, ByteOrder byteOrder);
	long getLong(int index);
	int getInt(int index, ByteOrder byteOrder);
	int getInt(int index);
	int parseNaturalIntAscii(int index, int length);
	long parseNaturalLongAscii(int index, int length);
	int parseIntAscii(int index, int length);
	long parseLongAscii(int index, int length);
	double getDouble(int index, ByteOrder byteOrder);
	double getDouble(int index);
	float getFloat(int index, ByteOrder byteOrder);
	float getFloat(int index);
	short getShort(int index, ByteOrder byteOrder);
	short getShort(int index);
	char getChar(int index, ByteOrder byteOrder);
	char getChar(int index);
	byte getByte(int index);
	void getBytes(int index, byte[] dst);
	void getBytes(int index, byte[] dst, int offset, int length);
	void getBytes(int index, MutableDirectBuffer dstBuffer, int dstIndex, int length);
	void getBytes(int index, ByteBuffer dstBuffer, int length);
	void getBytes(int index, ByteBuffer dstBuffer, int dstOffset, int length);
	String getStringAscii(int index);
	int getStringAscii(int index, Appendable appendable);
	String getStringAscii(int index, ByteOrder byteOrder);
	int getStringAscii(int index, Appendable appendable, ByteOrder byteOrder);
	String getStringAscii(int index, int length);
	int getStringAscii(int index, int length, Appendable appendable);
	String getStringWithoutLengthAscii(int index, int length);
	int getStringWithoutLengthAscii(int index, int length, Appendable appendable);
	String getStringUtf8(int index);
	String getStringUtf8(int index, ByteOrder byteOrder);
	String getStringUtf8(int index, int length);
	String getStringWithoutLengthUtf8(int index, int length);
	void boundsCheck(int index, int length);
	int wrapAdjustment();
}
