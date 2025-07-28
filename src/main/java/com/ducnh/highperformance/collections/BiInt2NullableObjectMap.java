package com.ducnh.highperformance.collections;

public class BiInt2NullableObjectMap<V> extends BiInt2ObjectMap<V> {
	public BiInt2NullableObjectMap() {
		super();
	}
	
	public BiInt2NullableObjectMap(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}
	
	protected Object mapNullValue(final Object value) {
		return value == null ? NullReference.INSTANCE : value;
	}
	
	@SuppressWarnings("unchecked")
	protected V unmapNullValue(final Object value) {
		return NullReference.INSTANCE == value ? null : (V)value;
	}
}
