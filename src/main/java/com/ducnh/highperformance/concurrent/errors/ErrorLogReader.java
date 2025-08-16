package com.ducnh.highperformance.concurrent.errors;

import com.ducnh.highperformance.concurrent.AtomicBuffer;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_INT;
import static com.ducnh.highperformance.BitUtil.align;
import static com.ducnh.highperformance.concurrent.errors.DistinctErrorLog.*;

public final class ErrorLogReader {
	private ErrorLogReader() {
		
	}
	
	public static boolean hasErros(final AtomicBuffer buffer) {
		return buffer.capacity() >= SIZE_OF_INT && buffer.getIntVolatile(LENGTH_OFFSET) > 0;
	}
	
	public static int read(final AtomicBuffer buffer, final ErrorConsumer consumer) {
		return read(buffer, consumer, 0);
	}
	
	public static int read(final AtomicBuffer buffer, final ErrorConsumer consumer, final long sinceTimestamp) {
		int entries = 0;
		int offset = 0;
		final int capacity = buffer.capacity();
		
		while (offset <= capacity - ENCODED_ERROR_OFFSET) {
			final int length = Math.min(buffer.getIntVolatile(offset + LENGTH_OFFSET), capacity - offset);
			if (length <= 0) {
				break;
			}
			
			final long lastObservationTimestamp = buffer.getLongVolatile(offset + LAST_OBSERVATION_TIMESTAMP_OFFSET);
			if (lastObservationTimestamp >= sinceTimestamp) {
				++entries;
				consumer.accept(
					buffer.getInt(offset + OBSERVATION_COUNT_OFFSET), 
					buffer.getLong(offset + FIRST_OBSERVATION_TIMESTAMP_OFFSET), 
					lastObservationTimestamp, 
					buffer.getStringWithoutLengthUtf8(offset + ENCODED_ERROR_OFFSET, length - ENCODED_ERROR_OFFSET));
			}
			
			offset += align(length, RECORD_ALIGNMENT);
		}
		
		return entries;
	}
}
