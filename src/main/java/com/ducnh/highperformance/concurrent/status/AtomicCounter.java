package com.ducnh.highperformance.concurrent.status;

public class AtomicCounter implements AutoCloseable{
	private boolean isClosed = false;
	private final int id;
	private final long addressOffset;
	private final byte[] byteArray;
	private CountersManager counterManager;
}
