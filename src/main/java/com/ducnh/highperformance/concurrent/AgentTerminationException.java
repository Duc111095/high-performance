package com.ducnh.highperformance.concurrent;

public class AgentTerminationException  extends RuntimeException{
	private static final long serialVersionUID = 5962977383701965069L;
	
	public AgentTerminationException() {
		
	}
	
	public AgentTerminationException(final String message) {
		super(message);
	}
	
	public AgentTerminationException(final String message, final Throwable cause) {
		super(message, cause);
	}
	
	public AgentTerminationException(final Throwable cause) {
		super(cause);
	}
	
	public AgentTerminationException(
		final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
