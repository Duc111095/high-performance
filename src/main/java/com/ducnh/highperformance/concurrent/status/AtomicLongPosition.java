package com.ducnh.highperformance.concurrent.status;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongPosition extends Position{
	private boolean isClosed = false;
	private final int id;
	private final AtomicLong value;
	
	public AtomicLongPosition() {
		this(0, 0L);
	}
	
	public AtomicLongPosition(final int id) {
		this(id, 0L);
	}
	
	public AtomicLongPosition(final int id, final long initialValue) {
		this.id = id;
		this.value = new AtomicLong(initialValue);
	}
	
	public boolean isClosed() {
		return isClosed;
	}
	
	public int id() {
		return id;
	}
	
	public long get() {
		return value.getPlain();
	}
	
	public long getVolatile() {
		return value.get();
	}
	
	public long getAcquire() {
		return value.getAcquire();
	}
	
	public long getOpaque() {
		return value.getOpaque();
	}
	
	public void set(final long value) {
		this.value.setPlain(value);
	}
	
	public void setOpaque(final long value) {
		this.value.setOpaque(value);
	}
	
	public void setOrdered(final long value) {
		setRelease(value);
	}
	
	public void setRelease(final long value) {
		this.value.setRelease(value);
	}
	
	public void setVolatile(final long value) {
		this.value.set(value);
	}
	
	public boolean proposeMax(final long proposedValue) {
		return proposeMaxRelease(proposedValue);
	}
	
	public boolean proposeMaxOrdered(final long proposedValue) {
		return proposeMaxRelease(proposedValue);
	}
	
	public boolean proposeMaxRelease(final long proposedValue) {
		boolean updated = false;
		
		if (value.get() < proposedValue) {
			value.setRelease(proposedValue);
			updated = true;
		}
		
		return updated;
	}
	
	public boolean proposeMaxOpaque(final long proposedValue) {
		boolean updated = false;
		if (value.get() < proposedValue) {
			value.setOpaque(proposedValue);
			updated = true;
		}
		
		return updated;
	}
	
	public void close() {
		isClosed = true;
	}
	
	public String toString() {
		return "AtomicLongPosition{" + 
			"isClosed=" + isClosed() +
			", id=" + id + 
			", value=" + (isClosed() ? -1 : value) + 
			"}";
	}
} 
