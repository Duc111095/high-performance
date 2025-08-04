package com.ducnh.highperformance.checksum;

@FunctionalInterface
public interface Checksum {
	int compute(long address, int offset, int length);
}
