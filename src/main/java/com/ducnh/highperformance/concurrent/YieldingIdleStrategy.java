package com.ducnh.highperformance.concurrent;

public final class YieldingIdleStrategy implements IdleStrategy{
	public YieldingIdleStrategy() {
		
	}
	
	public static final String ALIAS = "yield";
	public static final YieldingIdleStrategy INSTANCE = new YieldingIdleStrategy();
	
	public void idle(final int workCount) {
		if (workCount > 0) {
			return;
		}
		
		Thread.yield();
	}
	
	public void idle() {
		Thread.yield();
	}
	
	public void reset() {
	}
	
	public String alias() {
		return ALIAS;
	}
	
	public String toString() {
		return "YieldingIdleStrategy{alias=" + ALIAS + "}";
	}
} 
