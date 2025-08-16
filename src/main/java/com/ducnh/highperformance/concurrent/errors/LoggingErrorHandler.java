package com.ducnh.highperformance.concurrent.errors;

import java.io.PrintStream;
import java.util.Objects;

import com.ducnh.highperformance.ErrorHandler;

public class LoggingErrorHandler implements ErrorHandler, AutoCloseable{
	private volatile boolean isClosed;
	private final DistinctErrorLog log;
	private final PrintStream errorOverflow;
	
	public LoggingErrorHandler(final DistinctErrorLog log) {
		this(log, System.err);
	}
	
	public LoggingErrorHandler(final DistinctErrorLog log, final PrintStream errorOverflow) {
		Objects.requireNonNull(log, "log");
		Objects.requireNonNull(errorOverflow, "errorOverflow");
		
		this.log = log;
		this.errorOverflow = errorOverflow;
	}
	
	public void close() {
		isClosed = true;
	}
	
	public boolean isClosed() {
		return isClosed;
	}
	
	public DistinctErrorLog distinctErrorLog() {
		return log;
	}
	
	public PrintStream errorOverflow() {
		return errorOverflow;
	}
	
	public void onError(final Throwable throwable) {
		if (isClosed) {
			errorOverflow.println("error log is closed");
			throwable.printStackTrace(errorOverflow);
		} else if (!log.record(throwable)) {
			errorOverflow.println("error log is full, consider increasing length of error buffer");
			throwable.printStackTrace(errorOverflow);
		}
	}
}
