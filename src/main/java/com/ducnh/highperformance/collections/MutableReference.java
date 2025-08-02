package com.ducnh.highperformance.collections;

import java.util.Objects;

public class MutableReference<T> {
	
	public T ref;
	
	public MutableReference() {
		
	}
	
	public MutableReference(final T ref) {
		this.ref = ref;
	}
	
	public T get() {
		return ref;
	}
	
	public void set(final T ref) {
		this.ref = ref;
	}
	
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		
		final MutableReference<?> that = (MutableReference<?>) o;
		
		return Objects.equals(ref, that.ref);
	}
	
	public int hashCode() {
		return ref != null ? ref.hashCode() : 0;
	}
	
	public String toString() {
		return null == ref ? "null" : ref.toString();
	}
}
