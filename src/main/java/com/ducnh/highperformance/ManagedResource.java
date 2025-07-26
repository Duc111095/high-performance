package com.ducnh.highperformance;

public interface ManagedResource {
	void timeOfLastStateChange(long time);
	long timeOfLastStateChange();
	void delete();
}
