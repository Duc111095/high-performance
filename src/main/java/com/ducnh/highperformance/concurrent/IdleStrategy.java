package com.ducnh.highperformance.concurrent;

public interface IdleStrategy {
	void idle(int workout);
	void idle();
	void reset();
	default String alias() {
		return "";
	}
}
