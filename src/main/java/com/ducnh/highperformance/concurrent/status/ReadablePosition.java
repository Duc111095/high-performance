package com.ducnh.highperformance.concurrent.status;

public abstract class ReadablePosition implements AutoCloseable {
	public ReadablePosition() {
	}
	
	public abstract int id();
	public abstract long get();
	public abstract long getAcquire();
	public abstract long getOpaque();
	public abstract void close();
}
