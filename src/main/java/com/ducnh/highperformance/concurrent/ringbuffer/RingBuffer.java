package com.ducnh.highperformance.concurrent.ringbuffer;

import com.ducnh.highperformance.DirectBuffer;
import com.ducnh.highperformance.concurrent.AtomicBuffer;
import com.ducnh.highperformance.concurrent.ControlledMessageHandler;
import com.ducnh.highperformance.concurrent.MessageHandler;

public interface RingBuffer {
	int PADDING_MSG_TYPE_ID = -1;
	int INSUFFICIENT_CAPACITY = -2;
	int capacity();
	boolean write(int msgTypeId, DirectBuffer srcBuffer, int offset, int length);
	int tryClaim(int msgTypeId, int length);
	void commit(int index);
	void abort(int index);
	int read(MessageHandler handler);
	int read(MessageHandler handler, int messageCountLimit);
	int controlledRead(ControlledMessageHandler handler);
	int controlledRead(ControlledMessageHandler handler, int messageCountLimit);
	int maxMsgLength();
	long nextCorrelationId();
	AtomicBuffer buffer();
	void consumerHeartbeatTime(long time);
	long consumerHeartbeatTime();
	long producerPosition();
	long consumerPosition();
	int size();
	boolean unblock();
}
