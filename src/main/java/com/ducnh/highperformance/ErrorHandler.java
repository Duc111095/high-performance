package com.ducnh.highperformance;

@FunctionalInterface
public interface ErrorHandler {
	/**
	 * Callback to notify of an error that has occurred when processing an operation or event.
	 * @param throwable
	 */
	void onError(Throwable throwable);
}
