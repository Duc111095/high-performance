package com.ducnh.highperformance.concurrent;

import com.ducnh.highperformance.MutableDirectBuffer;

@FunctionalInterface
public interface MessageHandler {
	void onMessage(int msgTypeId, MutableDirectBuffer buffer, int index, int length);
}
