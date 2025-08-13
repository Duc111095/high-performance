package com.ducnh.highperformance.concurrent;

public final class BusySpinIdleStrategy {

	public static final String ALIAS = "spin";
	public static final BusySpinIdleStrategy INSTANCE = new BusySpinIdleStrategy();
	
	public BusySpinIdleStrategy() {
		
	}
	
	public void idle(final int workCount) {
		if (workCount > 0) {
			return;
		}
		
		Thread.onSpinWait();
	}
	
	public void idle() {
		Thread.onSpinWait();
	}
	
	public void reset() {
		
	}
	
	public String alias() {
		return ALIAS;
	}
	
	public String toString() {
		return "BusySpinIdleStrategy{alias=" + ALIAS + "}";
	}
}
