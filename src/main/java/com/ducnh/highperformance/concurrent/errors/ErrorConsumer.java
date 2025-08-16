package com.ducnh.highperformance.concurrent.errors;

@FunctionalInterface
public interface ErrorConsumer {
	
	void accept(
		int observationCount, long firstObservationTimestamp, long lastObservationTimestamp, String encodedException);
	
}
