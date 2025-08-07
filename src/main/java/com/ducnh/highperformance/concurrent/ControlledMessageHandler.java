package com.ducnh.highperformance.concurrent;

import com.ducnh.highperformance.MutableDirectBuffer;

@FunctionalInterface
public interface ControlledMessageHandler {
	enum Action {
		ABORT,
		BREAK,
		COMMIT,
		CONTINUE,
	}
	
	Action onMessage(int msgTypeId, MutableDirectBuffer buffer, int index, int length);
}
