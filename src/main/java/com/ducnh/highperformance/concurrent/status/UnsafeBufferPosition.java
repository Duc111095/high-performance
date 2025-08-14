package com.ducnh.highperformance.concurrent.status;

@SuppressWarnings("removal")
public class UnsafeBufferPosition extends Position{
	private boolean isClosed = false;
	private final int counterId;
	private final long addressOffset;
	private final byte[] byteArray;
	private final CountersManager counterManager;
}
