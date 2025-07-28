package com.ducnh.highperformance.collections;

public class NullReference {
	public static final NullReference INSTANCE = new NullReference();
	
	private NullReference() {
	}
	
	public int hashCode() {
		return 0;
	}
	
	public boolean equals(final Object obj) {
		return obj == this;
	}
}
