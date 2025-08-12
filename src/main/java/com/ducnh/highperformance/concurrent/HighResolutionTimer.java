package com.ducnh.highperformance.concurrent;

import java.util.concurrent.atomic.AtomicReference;

public final class HighResolutionTimer {
	
	private static final AtomicReference<Thread> THREAD = new AtomicReference<>();
	private HighResolutionTimer() {
		
	}
	
	public static boolean isEnabled() {
		return null != THREAD.get();
	}
	
	public static void enable() {
		if (null == THREAD.get()) {
			final Thread t = new Thread(HighResolutionTimer::run);
			if (THREAD.compareAndSet(null, t)) {
				t.setDaemon(true);
				t.setName("high-resolution-timer-hack");
				t.start();
			}
		}
	}
	
	public static void disable() {
		final Thread thread = THREAD.getAndSet(null);
		if (null != thread) {
			thread.interrupt();
		}
	}
	
	private static void run() {
		try {
			Thread.sleep(Long.MAX_VALUE);
		}
		catch (final InterruptedException ignore) {
			
		}
		
		THREAD.set(null);
	}
}
