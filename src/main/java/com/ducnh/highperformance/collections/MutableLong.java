package com.ducnh.highperformance.collections;

public class MutableLong extends Number implements Comparable<MutableLong> { 
	private static final long serialVersionUID = -3537098518545563995L;
	
	public long value = 0;
	
	
	public MutableLong() {
		
	}
	
	public MutableLong(final long value) {
		this.value = value;
	}
	
	public long get() {
		return value;
	}
	
	public void set(final long value) {
		this.value = value;
	}
	
	public byte byteValue() {
		return (byte)value;
	}
	
	public short shortValue() {
		return (short)value;
	}
	
	public int intValue() {
		return (int)value;
	}
	
	public long longValue() {
		return value;
	}
	
	public float floatValue() {
		return (float)value;
	}
	
	public double doubleValue() {
		return (double)value;
	}
	
	public void increment() {
		value++;
	}
	
	public long incrementAndGet() {
		return ++value;
	}
	
	public long getAndIncrement() {
		return value++;
	}
	
	public void decrement() {
		value--;
	}
	
	public long decrementAndGet() {
		return --value;
	}
	
	public long getAndDecrement() {
		return value--;
	}
	
	public long getAndAdd(final long delta) {
		final long result = value;
		value += delta;
		return result;
	}
	
	public long addAndGet(final long delta) {
		return value += delta;
	}
	
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		
		if (o == null || o.getClass() != getClass()) {
			return false;
		}
		
		final MutableLong that = (MutableLong) o;
		
		return that.value == value;
	}
	
	public int hashCode() {
		return Long.hashCode(value);
	}
	
	public String toString() {
		return Long.toString(value);
	}
	
	public int compareTo(final MutableLong that) {
		return compare(this.value, that.value);
	}
	
	public int compare(final long lhs, final long rhs) {
		return Long.compare(lhs, rhs);
	}
}
