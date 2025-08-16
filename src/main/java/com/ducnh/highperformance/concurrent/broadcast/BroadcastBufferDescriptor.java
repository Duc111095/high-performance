package com.ducnh.highperformance.concurrent.broadcast;

import static com.ducnh.highperformance.BitUtil.*;

public class BroadcastBufferDescriptor {
	
	public static final int TAIL_INTENT_COUNTER_OFFSET;
	
	public static final int TAIL_COUNTER_OFFSET;
	
	public static final int LATEST_COUNTER_OFFSET;
	
	public static final int TRAILER_LENGTH;
	
	static 
	{
		int offset = 0;
		TAIL_INTENT_COUNTER_OFFSET = offset;
		
		offset += SIZE_OF_LONG;
		TAIL_COUNTER_OFFSET = offset;
		
		offset += SIZE_OF_LONG;
		LATEST_COUNTER_OFFSET = offset;
		
		TRAILER_LENGTH = CACHE_LINE_LENGTH * 2;
	}
	
	private BroadcastBufferDescriptor() {
		
	}
	
	public static void checkCapacity(final int capacity) {
		if (!isPowerOfTwo(capacity)) {
			final String msg = "capacity must be a positive power of 2 + TRAILER_LENGTH: capacity=" + capacity;
			throw new IllegalStateException(msg);
		}
	}
}
