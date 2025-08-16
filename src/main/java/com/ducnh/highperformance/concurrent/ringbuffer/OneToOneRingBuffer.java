package com.ducnh.highperformance.concurrent.ringbuffer;

import static java.lang.Math.max;

import java.lang.invoke.VarHandle;

import com.ducnh.highperformance.DirectBuffer;
import com.ducnh.highperformance.concurrent.AtomicBuffer;
import com.ducnh.highperformance.concurrent.ControlledMessageHandler;
import com.ducnh.highperformance.concurrent.MessageHandler;

import static com.ducnh.highperformance.BitUtil.align;
import static com.ducnh.highperformance.concurrent.ControlledMessageHandler.Action.*;
import static com.ducnh.highperformance.concurrent.ringbuffer.RecordDescriptor.*;
import static com.ducnh.highperformance.concurrent.ringbuffer.RingBufferDescriptor.*;

public final class OneToOneRingBuffer implements RingBuffer {
	public static final int MIN_CAPACITY = HEADER_LENGTH * 2;
	
	private final int capacity;
	private final int maxMsgLength;
	private final int tailPositionIndex;
	private final int headCachePositionIndex;
	private final int headPositionIndex;
	private final int correlationIdCounterIndex;
	private final int consumerHeartbeatIndex;
	private final AtomicBuffer buffer;
	
	public OneToOneRingBuffer(final AtomicBuffer buffer) {
		capacity = checkCapacity(buffer.capacity(), MIN_CAPACITY);
		
		buffer.verifyAlignment();
		
		this.buffer = buffer;
		maxMsgLength = MIN_CAPACITY == capacity ? 0 : max(HEADER_LENGTH, capacity >> 3);
		tailPositionIndex = capacity + TAIL_POSITION_OFFSET;
		headCachePositionIndex = capacity + HEAD_CACHE_POSITION_OFFSET;
		headPositionIndex = capacity + HEAD_POSITION_OFFSET;
		correlationIdCounterIndex = capacity + CORRELATION_COUNTER_OFFSET;
		consumerHeartbeatIndex = capacity + CONSUMER_HEARTBEAT_OFFSET;
	}
	
	public int capacity() {
		return capacity;
	}
	
	public boolean write(final int msgTypeId, final DirectBuffer srcBuffer, final int offset, final int length) {
		checkTypeId(msgTypeId);
		checkMsgLength(length);
		
		final AtomicBuffer buffer = this.buffer;
		final int recordLength = length + HEADER_LENGTH;
		final int recordIndex = claimCapacity(buffer, recordLength);
		
		if (INSUFFICIENT_CAPACITY == recordIndex) {
			return false;
		}
		
		buffer.putIntRelease(lengthOffset(recordIndex), -recordLength);
		VarHandle.releaseFence();
		
		buffer.putBytes(encodedMsgOffset(recordIndex), srcBuffer, offset, length);
		buffer.putInt(typeOffset(recordIndex), msgTypeId);
		buffer.putIntRelease(lengthOffset(recordIndex), recordLength);
	
		return true;
	}
	
	public int tryClaim(final int msgTypeId, final int length) {
		checkTypeId(msgTypeId);
		checkMsgLength(length);
		
		final AtomicBuffer buffer = this.buffer;
		final int recordLength = length + HEADER_LENGTH;
		final int recordIndex = claimCapacity(buffer, recordLength);
		
		if (INSUFFICIENT_CAPACITY == recordIndex) {
			return recordIndex;
		}
		
		buffer.putIntRelease(lengthOffset(recordIndex), -recordLength);
		VarHandle.releaseFence();
		buffer.putInt(typeOffset(recordIndex), msgTypeId);
		
		return encodedMsgOffset(recordIndex);
	}
	
	public void commit(final int index) {
		final int recordIndex = computeRecordIndex(index);
		final AtomicBuffer buffer = this.buffer;
		final int recordLength = verifyClaimedSpaceNotReleased(buffer, recordIndex);
		
		buffer.putIntRelease(lengthOffset(recordIndex), -recordLength);
	}
	
	public void abort(final int index) {
		final int recordIndex = computeRecordIndex(index);
		final AtomicBuffer buffer = this.buffer;
		final int recordLength = verifyClaimedSpaceNotReleased(buffer, recordIndex);
		
		buffer.putInt(typeOffset(recordIndex), PADDING_MSG_TYPE_ID);
		buffer.putIntRelease(lengthOffset(recordIndex), -recordLength);
	}
	
	public int read(final MessageHandler handler) {
		return read(handler, Integer.MAX_VALUE);
	}
	
	
	public int read(final MessageHandler handler, final int messageCountLimit) {
		int messagesRead = 0;
		
		final AtomicBuffer buffer = this.buffer;
		final int headPositionIndex = this.headPositionIndex;
		final long head = buffer.getLong(headPositionIndex);
		
		int bytesRead = 0;
		
		final int capacity = this.capacity;
		final int headIndex = (int)head & (capacity - 1);
		final int contiguounsBlockLength = capacity - headIndex;
		
		try {
			while ((bytesRead < contiguounsBlockLength) && (messagesRead < messageCountLimt)) {
				final int recordIndex = headIndex + bytesRead;
				final int recordLength = buffer.getIntVolatile(lengthOffset(recordIndex));
				
				if (recordLength < 0) {
					break;
				}
				
				bytesRead += align(recordLength, ALIGNMENT);
				
				final int messageTypeId = buffer.getInt(typeOffset(recordIndex));
				if (PADDING_MSG_TYPE_ID == messageTypeId) {
					continue;
				}
				
				handler.onMessage(messageTypeId, buffer, recordIndex + HEADER_LENGTH, recordLength - HEADER_LENGTH);
				++messagesRead;
			}
		} finally {
			if (bytesRead > 0) {
				buffer.putLongRelease(headPositionIndex, head + bytesRead);
			}
		}
		
		return messagesRead;
	}
	
	public int controlledRead(final ControlledMessageHandler handler) {
		return controlledRead(handler, Integer.MAX_VALUE);
	}
	
	public int controlledRead(final ControlledMessageHandler handler, final int messageCountLimit) {
		int messagesRead = 0;
		
		final AtomicBuffer buffer = this.buffer;
		final int headPositionIndex = this.headPositionIndex;
		long head = buffer.getLong(headPositionIndex);
		
		int bytesRead = 0;
		
		final int capacity = this.capacity;
		int headIndex = (int)head & (capacity - 1);
		final int contigunousBlockLength = capacity - headIndex;
		
		try {
			while ((bytesRead < contigunousBlockLength) && (messagesRead < messageCountLimit)) {
				final int recordIndex = headIndex + bytesRead;
				final int recordLength = buffer.getIntVolatile(lengthOffset(recordIndex));
				if (recordLength < 0) {
					break;
				}
				
				final int alignedLength = align(recordLength, ALIGNMENT);
				bytesRead += alignedLength;
				
				final int messageTypeId = buffer.getInt(typeOffset(recordIndex));
				if (PADDING_MSG_TYPE_ID == messageTypeId) {
					continue;
				}
				
				final ControlledMessageHandler.Action action = handler.onMessage(
						messageTypeId, buffer, recordIndex + HEADER_LENGTH, recordLength - HEADER_LENGTH);
			
				if (ABORT == action) {
					bytesRead -= alignedLength;
					break;
				}
				
				++messagesRead;
				
				if (BREAK == action) {
					break;
				}
				
				if (COMMIT == action) {
					buffer.putLongRelease(headPositionIndex, head + bytesRead);
					headIndex += bytesRead;
					head += bytesRead;
					bytesRead = 0;
				}
			}
		}
		finally 
		{
			if (bytesRead > 0) {
				buffer.putLongRelease(headPositionIndex, head + bytesRead);
			}
		}
		
		return messagesRead;
	}
	
	public int maxMsgLength() {
		return maxMsgLength;
	}
	
	public long nextCorrelationId() {
		return buffer.getAndAddLong(correlationIdCounterIndex, 1);
	}
	
	public AtomicBuffer buffer() {
		return buffer;
	}
	
	public void consumerHeartbeatTime(final long time) {
		buffer.putLongVolatile(consumerHeartbeatIndex, time);
	}
	
	public long consumerHeartbeatTime() {
		return buffer.getLongVolatile(consumerHeartbeatIndex);
	}
	
	public long producerPosition() {
		return buffer.getLongVolatile(tailPositionIndex);
	}
	
	public long consumerPosition() {
		return buffer.getLongVolatile(headPositionIndex);
	}
	
	public int size() {
		final AtomicBuffer buffer = this.buffer;
		final int headPositionIndex = this.headPositionIndex;
		final int tailPositionIndex = this.tailPositionIndex;
		long headBefore;
		long tail;
		long headAfter = buffer.getLongVolatile(headPositionIndex);
		
		do {
			headBefore = headAfter;
			tail = buffer.getLongVolatile(tailPositionIndex);
			headAfter = buffer.getLongVolatile(headPositionIndex);
		}
		while (headAfter != headBefore);
		
		final long size = tail - headAfter;
		if (size < 0) {
			return 0;
		} else  if (size > capacity) {
			return capacity;
		}
		
		return (int)size;
	}
	
	public boolean unblock() {
		return false;
	}
	
	private void checkMsgLength(final int length) {
		if (length < 0) {
			throw new IllegalArgumentException("invalid message length=" + length);
		}
		else if (length > maxMsgLength) {
			throw new IllegalArgumentException("encoded message exceeds maxMsgLength=" 
					+ maxMsgLength + ", length=" + length);
		}
	}
	
	private int claimCapacity(final AtomicBuffer buffer, final int recordLength) {
		final int alignedRecordLength = align(recordLength, ALIGNMENT);
		final int requiredCapacity =alignedRecordLength + HEADER_LENGTH;
		final int capacity = this.capacity;
		final int tailPositionIndex = this.tailPositionIndex;
		final int headCachePositionIndex = this.headCachePositionIndex;
		final int mask = capacity - 1;
		
		long head = buffer.getLong(headCachePositionIndex);
		final long tail = buffer.getLong(tailPositionIndex);
		final int availableCapacity = capacity - (int)(tail - head);
		
		if (requiredCapacity > availableCapacity) {
			head = buffer.getLongVolatile(headPositionIndex);
			
			if (requiredCapacity > (capacity - (int)(tail - head))) {
				return INSUFFICIENT_CAPACITY;
			}
			
			buffer.putLong(headCachePositionIndex, head);
		}
		
		int padding = 0;
		final int recordIndex = (int) tail & mask;
		final int toBufferEndLength = capacity - recordIndex;
		int writeIndex = recordIndex;
		long nextTail = tail + alignedRecordLength;
		
		if (alignedRecordLength == toBufferEndLength) {
			buffer.putLongRelease(tailPositionIndex, nextTail);
			buffer.putLong(0, 0L);
			return recordIndex;
		}
		else if (requiredCapacity > toBufferEndLength) {
			writeIndex = 0;
			int headIndex = (int)head & mask;
			if (requiredCapacity > headIndex) {
				head = buffer.getLongVolatile(headPositionIndex);
				headIndex = (int)head & mask;
				if (requiredCapacity > headIndex) {
					writeIndex = INSUFFICIENT_CAPACITY;
					nextTail = tail;
				}
				
				buffer.putLong(headCachePositionIndex, head);
			}
			
			padding = toBufferEndLength;
			nextTail += padding;
		}
		
		buffer.putLongRelease(tailPositionIndex, nextTail);
		
		if (0 != padding) {
			buffer.putLong(0, 0L);
			buffer.putIntRelease(lengthOffset(recordIndex), -padding);
			VarHandle.releaseFence();
			
			buffer.putInt(typeOffset(recordIndex), PADDING_MSG_TYPE_ID);
			buffer.putIntRelease(lengthOffset(recordIndex), padding);
		}
		
		if (INSUFFICIENT_CAPACITY != writeIndex) {
			buffer.putLong(writeIndex + alignedRecordLength, 0L);
		}
		
		return writeIndex;
	}
	
	private int computeRecordIndex(final int index) {
		final int recordIndex = index - HEADER_LENGTH;
		if (recordIndex < 0 | recordIndex > (capacity - HEADER_LENGTH)) {
			throw new IllegalArgumentException("invalid message index " + index);
		}
		
		return recordIndex;
	}
	
	private int verifyClaimedSpaceNotReleased(final AtomicBuffer buffer, final int recordIndex) {
		final int recordLength = buffer.getInt(lengthOffset(recordIndex));
		if (recordLength < 0) {
			return recordLength;
		}
		
		throw new IllegalStateException("claimed space previously " + 
				(PADDING_MSG_TYPE_ID == buffer.getInt(typeOffset(recordIndex)) ? "aborted" : "committed"));
	}
}
