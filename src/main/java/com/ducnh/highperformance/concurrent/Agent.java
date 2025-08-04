package com.ducnh.highperformance.concurrent;

public interface Agent {
	default void onStart() {};
	int doWork() throws Exception;
	default void onClose() {};
	String roleName();
}
