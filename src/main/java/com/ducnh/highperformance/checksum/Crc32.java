package com.ducnh.highperformance.checksum;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.zip.CRC32;

import com.ducnh.highperformance.LangUtil;

public class Crc32 implements Checksum{

	private static final Crc32 INSTANCE = new Crc32();
	
	private static final MethodHandle UPDATE_BYTE_BUFFER;
	
	static {
		try {
			final Method method = 
					CRC32.class.getDeclaredMethod("updateByteBuffer0", int.class, long.class, int.class, int.class);
			method.setAccessible(true);
			MethodHandle methodHandle = MethodHandles.lookup().unreflect(method);
			methodHandle = MethodHandles.insertArguments(methodHandle, 0, 0);
			UPDATE_BYTE_BUFFER = methodHandle;
		} catch (final Exception ex) {
			throw new Error(ex);
		}
	}
	
	private Crc32() {
		
	}
	
	public int compute(final long address, final int offset, final int length) {
		try {
			return (int)UPDATE_BYTE_BUFFER.invokeExact(address, offset, length);
		} catch (final Throwable t) {
			LangUtil.rethrowUnchecked(t);
			return -1;
		}
	}
}
