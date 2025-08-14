package com.ducnh.highperformance.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class NoOpLock implements Lock{
	public static final NoOpLock INSTANCE = new NoOpLock();
	
	public NoOpLock() {
		
	}
	
	public void lock() {
		
	}
	
	public void lockInterruptibly() {
		
	}
	
	public boolean tryLock() {
		return true;
	}
	
	public boolean tryLock(final long time, final TimeUnit unit) {
		return true;
	}
	
	public void unlock() {
		
	}
	
	public Condition newCondition() {
		throw new UnsupportedOperationException();
	}
}
