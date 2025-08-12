package com.ducnh.highperformance.concurrent.status;

public abstract class StatusIndicator  extends StatusIndicatorReader{
	
	public StatusIndicator() {	
	}
	
	public abstract void setVolatile(long value);
	public abstract void setOrdered(long value);
	public abstract void setRelease(long value);
	public abstract void setOpaque(long value);
}
