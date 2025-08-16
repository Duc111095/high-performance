package com.ducnh.highperformance.concurrent.broadcast;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_INT;

public final class RecordDescriptor {
	
	public static final int PADDING_MSG_TYPE_ID = -1;
	
	public static final int LENGTH_OFFSET = 0;
	
	public static final int TYPE_OFFSET = LENGTH_OFFSET + SIZE_OF_INT;
	
	public static final int HEADER_LENGTH = SIZE_OF_INT * 2;
	
	public static final int RECORD_ALIGNMENT = HEADER_LENGTH;
	
	private RecordDescriptor() {
		
	}
	
	public static int calculateMaxMessageLength(final int capacity) {
		return capacity / 8;
	}
	
	public static int lengthOffset(final int recordOffset) {
		return recordOffset + LENGTH_OFFSET; 
	}
	
	public static int typeOffset(final int recordOffset) {
		return recordOffset + TYPE_OFFSET;
	}
	
	public static int msgOffset(final int recordOffset) {
		return recordOffset + HEADER_LENGTH;
	}
	
	public static void checkTypeId(final int msgTypeId) {
		if (msgTypeId < 1) {
			final String msg = "type id must be greater than zero, msgTypeId="  + msgTypeId;
			throw new IllegalArgumentException(msg);
		}
	}
}
