package com.ducnh.highperformance.concurrent.broadcast;

import java.nio.ByteBuffer;

import com.ducnh.highperformance.MutableDirectBuffer;
import com.ducnh.highperformance.concurrent.MessageHandler;
import com.ducnh.highperformance.concurrent.UnsafeBuffer;

public class CopyBroadcastReceiver {
	
	public static final int SCRATCH_BUFFER_LENGTH = 4096;
	
	private final BroadcastReceiver receiver;
	private final MutableDirectBuffer scratchBuffer;
	
	public CopyBroadcastReceiver(final BroadcastReceiver receiver, final MutableDirectBuffer scratchBuffer) {
		this.receiver = receiver;
		this.scratchBuffer = scratchBuffer;
	}
	
	public BroadcastReceiver broadcastReceiver() {
		return receiver;
	}
	
	public CopyBroadcastReceiver(final BroadcastReceiver receiver, final int scratchBufferLength) {
		this.receiver = receiver;
		scratchBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(scratchBufferLength));
	}
	
	public CopyBroadcastReceiver(final BroadcastReceiver receiver) {
		this(receiver, SCRATCH_BUFFER_LENGTH);
	}
	
	public int receive(final MessageHandler handler) {
		int messagesReceived = 0;
		final BroadcastReceiver receiver = this.receiver;
		final long lastSeenLappedCount = receiver.lappedCount();
		
		if (receiver.receiveNext()) {
			if (lastSeenLappedCount != receiver.lappedCount()) {
				throw new IllegalStateException("unable to keep up with broadcast");
			}
			
			final int length = receiver.length();
			final int capacity = scratchBuffer.capacity();
			if (length > capacity && !scratchBuffer.isExpandable()) {
				throw new IllegalStateException(
					"buffer required length of " + length + " but only has " + capacity);
			}
			
			final int msgTypeId = receiver.typeId();
			scratchBuffer.putBytes(0, receiver.buffer(), receiver.offset(), length);
			
			if (!receiver.validate()) {
				throw new IllegalStateException("unable to keep up with broadcast");
			}
			
			handler.onMessage(msgTypeId, scratchBuffer, 0, length);
			
			messagesReceived = 1;
 		}
		
		return messagesReceived;
	}
}
