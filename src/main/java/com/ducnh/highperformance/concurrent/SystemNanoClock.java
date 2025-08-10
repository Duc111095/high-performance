package com.ducnh.highperformance.concurrent;

public class SystemNanoClock implements NanoClock{
	public SystemNanoClock() {
		
	}
	
	public static final SystemNanoClock INSTANCE = new SystemNanoClock();
	
	public long nanoTime() {
		return System.nanoTime();
	}
}
