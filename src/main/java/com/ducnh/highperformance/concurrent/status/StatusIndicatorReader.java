package com.ducnh.highperformance.concurrent.status;

public abstract class StatusIndicatorReader {
	public StatusIndicatorReader() {
		
	}
	
	public abstract int id();
	public abstract long getVolatile();
	public abstract long getAcquire();
	public abstract long getOpaque();
}
