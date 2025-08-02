package com.ducnh.highperformance.collections;

public class MutableBoolean {
	
	public boolean value;
	
	public MutableBoolean() {
		
	}
	
	public MutableBoolean(final boolean value) {
		this.value = value;
	}
	
	public boolean get() {
		return value;
	}
	
	public void set(final boolean value) {
		this.value = value;
	}
	
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		
		final MutableBoolean that = (MutableBoolean) o;
		
		return value == that.value;
	}
	
	public int hashCode() {
		return Boolean.hashCode(value);
	}
	
	public String toString() {
		return Boolean.toString(value);
	}
}
