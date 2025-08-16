package com.ducnh.highperformance.concurrent.broadcast;

import com.ducnh.highperformance.concurrent.AtomicBuffer;

import static com.ducnh.highperformance.concurrent.broadcast.BroadcastBufferDescriptor.*;
import static com.ducnh.highperformance.concurrent.broadcast.RecordDescriptor.*;

import java.lang.invoke.VarHandle;

import com.ducnh.highperformance.BitUtil;
import com.ducnh.highperformance.DirectBuffer;

public class BroadcastTransmitter {
	private final AtomicBuffer buffer;
	private final int capacity;
	private final int maxMsgLength;
	private final int tailIntentCountIndex;
	private final int tailCounterIndex;
	private final int latestCounterIndex;
	
	public BroadcastTransmitter(final AtomicBuffer buffer) {
		this.buffer = buffer;
		this.capacity = buffer.capacity() - TRAILER_LENGTH;
		
		checkCapacity(capacity);
		buffer.verifyAlignment();
		
		this.maxMsgLength = calculateMaxMessageLength(capacity);
		this.tailIntentCountIndex = capacity + TAIL_INTENT_COUNTER_OFFSET;
		this.tailCounterIndex = capacity + TAIL_COUNTER_OFFSET;
		this.latestCounterIndex = capacity + LATEST_COUNTER_OFFSET;
	}
	
	public int capacity() {
		return capacity;
	}
	
	public int maxMsgLength() {
		return maxMsgLength;
	}
	
	public void transmit(final int msgTypeId, final DirectBuffer srcBuffer, final int srcIndex, final int length) {
		checkTypeId(msgTypeId);
		checkMessageLength(length);
		
		final AtomicBuffer buffer = this.buffer;
		long currentTail = buffer.getLong(tailCounterIndex);
		int recordOffset = (int)currentTail & (capacity - 1);
		final int recordLength = HEADER_LENGTH + length;
		final int recordLengthAligned = BitUtil.align(recordLength, RECORD_ALIGNMENT);
		final long newTail = currentTail + recordLengthAligned;
		
		final int toEndOfBuffer = capacity - recordOffset;
		if (toEndOfBuffer < recordLengthAligned) {
			signalTailIntent(buffer, newTail + toEndOfBuffer);
			insertPaddingRecord(buffer, recordOffset, toEndOfBuffer);
			
			currentTail += toEndOfBuffer;
			recordOffset = 0;
		} else {
			signalTailIntent(buffer, newTail);
		}
		
		buffer.putInt(lengthOffset(recordOffset), recordLength);
		buffer.putInt(typeOffset(recordOffset), msgTypeId);
		
		buffer.putBytes(msgOffset(recordOffset), srcBuffer, srcIndex, length);
		
		buffer.putLongRelease(latestCounterIndex, currentTail);
		buffer.putLongRelease(tailCounterIndex, currentTail + recordLengthAligned); 
	}
	
	private void signalTailIntent(final AtomicBuffer buffer, final long newTail) {
		buffer.putLongRelease(tailCounterIndex, newTail);
		VarHandle.releaseFence();
	}
	
	private static void insertPaddingRecord(final AtomicBuffer buffer, final int recordOffset, final int length) {
		buffer.putInt(lengthOffset(recordOffset), length);
		buffer.putInt(typeOffset(recordOffset), PADDING_MSG_TYPE_ID);
	}
	
	private void checkMessageLength(final int length) {
		if (length > maxMsgLength) {
			throw new IllegalArgumentException(
				"encoded message exceeds maxMsgLength of " + maxMsgLength + ", length=" + length);
		}
	}
}
