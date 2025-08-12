package com.ducnh.highperformance.concurrent;

public class NoOpIdleStrategy implements IdleStrategy{
	public static final String ALIAS = "noop";
	public static final NoOpIdleStrategy INSTANCE = new NoOpIdleStrategy();
	
	public NoOpIdleStrategy() {	
	}
	
	public void idle(final int workCount) {
	}
	
	public void idle() {
	}
	
	public void reset() {
		
	}
	
	public String alias() {
		return ALIAS;
	}
	
	public String toString() {
		return "NoOpIdleStrategy{alias=" + ALIAS + "}";
	}
}
