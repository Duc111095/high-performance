package com.ducnh.highperformance.concurrent;

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
