package com.ducnh.highperformance.collections;

public class MutableInteger extends Number implements Comparable<MutableInteger>{
	private static final long serialVersionUID = 985259236882848264L;
	
	public int value = 0;
	
	public MutableInteger() {
		
	}
	
	public MutableInteger(final int value) {
		this.value = value;
	}
	
	public int get() {
		return value;
	}
	
	public void set(final int value) {
		this.value = value;
	}
	
	public byte byteValue() {
		return (byte)value;
	}
	
	public short shortValue() {
		return (short)value;
	}
	
	public int intValue() {
		return value;
	}
	
	public long longValue() {
		return value;
	}
	
	public float floatValue() {
		return (float)value;
	}
	
	public double doubleValue() {
		return (double) value;
	}
	
	public void increment() {
		value++;
	}
	
	public int incrementAndGet() {
		return ++value;
	}
	
	public int getAndIncrement() {
		return value++;
	}
	
	public void decrement() {
		value--;
	}
	
	public int decrementAndGet() {
		return --value;
	}
	
	public int getAndDecrement() {
		return value--;
	}
	
	public int getAndAdd(final int delta) {
		final int result = value;
		value += delta;
		return result;
	}
	
	public int addAndGet(final int delta) {
		return value += delta;
	}
	
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		
		final MutableInteger that = (MutableInteger) o;
		
		return value == that.value;
	}
	
	public int hashCode() {
		return Integer.hashCode(value);
	}
	
	public String toString() {
		return Integer.toString(value);
	}
	
	public int compareTo(final MutableInteger that) {
		return compare(this.value, that.value);
	}
	
	public static int compare(final int lhs, final int rhs) {
		return Integer.compare(lhs, rhs);
	}
}
