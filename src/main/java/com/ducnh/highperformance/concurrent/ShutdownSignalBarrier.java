package com.ducnh.highperformance.concurrent;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class ShutdownSignalBarrier {
	
	private static final String[] SIGNAL_NAMES = {"INT", "TERM"};
	private static final ArrayList<CountDownLatch> LATCHES = new ArrayList<>();

	static {
		final Runnable handler = ShutdownSignalBarrier::signalAndClearAll;
		for (final String name : SIGNAL_NAMES) {
			SigInt.register(name, handler);
		}
	}
	
	private final CountDownLatch latch = new CountDownLatch(1);
	
	public ShutdownSignalBarrier() {
		synchronized (LATCHES) {
			LATCHES.add(latch);
		}
 	}
	
	public void signal() {
		synchronized (LATCHES) {
			LATCHES.remove(latch);
			latch.countDown();
		}
 	}
	
	public void signalAll() {
		signalAndClearAll();
	}
	
	public void remove() {
		synchronized (LATCHES) {
			LATCHES.remove(latch);
		}
	}
	
	public void await() {
		try {
			latch.await();
		} catch (final InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
	
	private static void signalAndClearAll() {
		synchronized (LATCHES) {
			LATCHES.forEach(CountDownLatch::countDown);
			LATCHES.clear();
		}
	}
}
