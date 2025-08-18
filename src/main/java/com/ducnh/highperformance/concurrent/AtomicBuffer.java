package com.ducnh.highperformance.concurrent;

import com.ducnh.highperformance.MutableDirectBuffer;
import com.ducnh.highperformance.SystemUtil;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_LONG;

public interface AtomicBuffer extends MutableDirectBuffer{
	int ALIGNMENT = SIZE_OF_LONG;
	String STRICT_ALIGNMENT_CHECKS_PROP_NAME = "com.ducnh.strict.alignment.checks";
	boolean STRICT_ALIGNMENT_CHECKS = !SystemUtil.isX64Arch() ||
			"true".equals(SystemUtil.getProperty(STRICT_ALIGNMENT_CHECKS_PROP_NAME, "false"));
	
	void verifyAlignment();
	long getLongVolatile(int index);
	long getLongAcquire(int index);
	void putLongVolatile(int index, long value);
	void putLongOrdered(int index, long value);
	void putLongRelease(int index, long value);
	long addLongOrdered(int index, long increment);
	void putLongOpaque(int index, long value);
	long getLongOpaque(int index);
	long addLongOpaque(int index, long increment);
	long addLongRelease(int index, long increment);
	boolean compareAndSetLong(int index, long expectedValue, long updatedValue);
	long compareAndExchangeLong(int index, long expectedValue, long updatedValue);
	long getAndSetLong(int index, long value);
	long getAndAddLong(int index, long delta);
	int getIntVolatile(int index);
	void putIntVolatile(int index, int value);
	int getIntAcquire(int index);
	void putIntOrdered(int index, int value);
	void putIntRelease(int index, int value);
	int addIntOrdered(int index, int increment);
	int addIntRelease(int index, int increment);
	void putIntOpaque(int index, int value);
	int getIntOpaque(int index);
	int addIntOpaque(int index, int increment);
	int compareAndExchangeInt(int index, int expectedValue, int updatedValue);
	int getAndSetInt(int index, int value);
	int getAndAddInt(int index, int value);
	short getShortVolatile(int index);
	void putShortVolatile(int index, short value);
	char getCharVolatile(int index);
	void putCharVolatile(int index, char value);
	byte getByteVolatile(int index);
	void putByteVolatile(int index, byte value);
}