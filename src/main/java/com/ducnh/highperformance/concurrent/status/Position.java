package com.ducnh.highperformance.concurrent.status;

public abstract class Position extends ReadablePosition {
	public Position() {
		
	}
	public abstract boolean isClosed();
	public abstract void setVolatile(long value);
	public abstract void setOrdered(long value);
	public abstract void setRelease(long value);
	public abstract void setOpaque(long value);
	public abstract void set(long value);
	public abstract boolean proposeMax(long proposedValue);
	public abstract boolean proposeMaxOrdered(long proposedValue);
	public abstract boolean proposeMaxRelease(long proposedValue);
	public abstract boolean proposeMaxOpaque(long proposedValue);
}
