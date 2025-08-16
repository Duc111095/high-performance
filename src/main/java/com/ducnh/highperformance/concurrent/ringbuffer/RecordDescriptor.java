package com.ducnh.highperformance.concurrent.ringbuffer;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_INT;

public final class RecordDescriptor {
	
	public static final int HEADER_LENGTH = SIZE_OF_INT * 2;
	public static final int ALIGNMENT = HEADER_LENGTH;
	
	private RecordDescriptor() {
		
	}
	
	public static int lengthOffset(final int recordOffset) {
		return recordOffset;
	}
	
	public static int typeOffset(final int recordOffset) {
		return recordOffset + SIZE_OF_INT;
	}
	
	public static int encodedMsgOffset(final int recordOffset) {
		return recordOffset + HEADER_LENGTH;
	}
	
	public static void checkTypeId(final int msgTypeId) {
		if (msgTypeId < 1) {
			final String msg = "message type id must be greater than zero, msgTypeId=" + msgTypeId;
			throw new IllegalArgumentException(msg);
		}
	}
}
