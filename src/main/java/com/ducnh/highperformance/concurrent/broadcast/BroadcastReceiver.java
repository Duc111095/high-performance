package com.ducnh.highperformance.concurrent.broadcast;

import static com.ducnh.highperformance.BitUtil.align;
import static com.ducnh.highperformance.concurrent.broadcast.BroadcastBufferDescriptor.*;
import static com.ducnh.highperformance.concurrent.broadcast.RecordDescriptor.*;

import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicLong;

import com.ducnh.highperformance.MutableDirectBuffer;
import com.ducnh.highperformance.concurrent.AtomicBuffer;

public class BroadcastReceiver {
	private long cursor;
	private long nextRecord;
	private int recordOffset;
	
	private final int capacity;
	private final int tailIntentCounterIndex;
	private final int tailCounterIndex;
	
	private final int latestCounterIndex;
	private final AtomicBuffer buffer;
	private final AtomicLong lappedCount = new AtomicLong();
	
	public BroadcastReceiver(final AtomicBuffer buffer) {
		this.buffer = buffer;
		this.capacity = buffer.capacity() - TRAILER_LENGTH;
		
		checkCapacity(capacity);
		buffer.verifyAlignment();
		
		tailIntentCounterIndex = capacity + TAIL_INTENT_COUNTER_OFFSET;
		tailCounterIndex = capacity + TAIL_COUNTER_OFFSET;
		latestCounterIndex = capacity + LATEST_COUNTER_OFFSET;
		
		cursor = nextRecord = buffer.getLongVolatile(latestCounterIndex);
		recordOffset = (int)cursor & (capacity - 1);
	}
	
	public int capacity() {
		return capacity;
	}
	
	public long lappedCount() {
		return lappedCount.get();
	}
	
	public int typeId() {
		return buffer.getInt(typeOffset(recordOffset));
	}
	
	public int offset() {
		return msgOffset(recordOffset);
	}
	
	public int length() {
		return buffer.getInt(lengthOffset(recordOffset)) - HEADER_LENGTH;
	}
	
	public MutableDirectBuffer buffer() {
		return buffer;
	}
	
	public boolean receiveNext() {
		boolean isAvailable = false;
		final AtomicBuffer buffer = this.buffer;
		final long tail = buffer.getLongVolatile(tailCounterIndex);
		long cursor = nextRecord;
		
		if (tail > cursor) {
			final int capacity = this.capacity;
			int recordOffset = (int)cursor & (capacity - 1);
			
			if (!validate(cursor, buffer, capacity)) {
				lappedCount.lazySet(lappedCount.get() + 1);
				
				cursor = buffer.getLongVolatile(latestCounterIndex);
				recordOffset = (int)cursor & (capacity - 1);
			}
			
			this.cursor = cursor;
			nextRecord = cursor + align(buffer.getInt(lengthOffset(recordOffset)), RECORD_ALIGNMENT);
			
			if (PADDING_MSG_TYPE_ID == buffer.getInt(typeOffset(recordOffset))) {
				recordOffset = 0;
				this.cursor = nextRecord;
				nextRecord += align(buffer.getInt(lengthOffset(recordOffset)), RECORD_ALIGNMENT);
			}
			
			this.recordOffset = recordOffset;
			isAvailable = true;
		}
		
		return isAvailable;
	}
	
	public boolean validate() {
		VarHandle.acquireFence();
		return validate(cursor, buffer, capacity);
	}
	
	private boolean validate(final long cursor, final AtomicBuffer buffer, final int capacity) {
		return (cursor + capacity) > buffer.getLongVolatile(tailIntentCounterIndex);
	}
}
