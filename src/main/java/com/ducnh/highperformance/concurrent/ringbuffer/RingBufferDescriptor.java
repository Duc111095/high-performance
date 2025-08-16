package com.ducnh.highperformance.concurrent.ringbuffer;

import static com.ducnh.highperformance.BitUtil.*;

import com.ducnh.highperformance.BitUtil;

public final class RingBufferDescriptor {
	
	public static final int TAIL_POSITION_OFFSET;
	public static final int HEAD_CACHE_POSITION_OFFSET;
	public static final int HEAD_POSITION_OFFSET;
	public static final int CORRELATION_COUNTER_OFFSET;
	public static final int CONSUMER_HEARTBEAT_OFFSET;
	public static final int TRAILER_LENGTH;
	
	static {
		int offset = 0;
		offset += (CACHE_LINE_LENGTH * 2);
		TAIL_POSITION_OFFSET = offset;
		
		offset += (CACHE_LINE_LENGTH * 2);
		HEAD_CACHE_POSITION_OFFSET = offset;
		
		offset += (CACHE_LINE_LENGTH * 2);
		HEAD_POSITION_OFFSET = offset;
		
		offset += (CACHE_LINE_LENGTH * 2);
		CORRELATION_COUNTER_OFFSET = offset;
		
		offset += (CACHE_LINE_LENGTH * 2);
		CONSUMER_HEARTBEAT_OFFSET = offset;
		
		offset += (CACHE_LINE_LENGTH * 2);
		TRAILER_LENGTH = offset;
	}
	
	private RingBufferDescriptor() {
		
	}
	
	public static int checkCapacity(final int capacity, final int minCapacity) {
		final int dataCapacity = capacity - TRAILER_LENGTH;
		if (!BitUtil.isPowerOfTwo(dataCapacity)) {
			throw new IllegalArgumentException(
				"capacity must be a positive power of 2 + TRAILER_LENGTH: capacity=" + capacity);
		}
		if (dataCapacity < minCapacity) {
			throw new IllegalArgumentException(
				"insufficient capacity: minCapacity=" + (minCapacity + TRAILER_LENGTH) + ", capacity=" + capacity);
		}
		
		return dataCapacity;
	}
}
