package com.ducnh.highperformance;

import java.util.Map;

public class Verify {
	private Verify() {}
	
	public static void notNull(final Object ref, final String name) {
		if (ref == null) {
			throw new NullPointerException(name + " must not be null");
		}
	}
	
	public static void verifyNull(final Object ref, final String name) {
		if (ref != null) {
			throw new NullPointerException(name + " must be null");
		}
	}
	
	public static void present(final Map<?, ?> map, final Object key, final String name) {
		if (map.get(key) == null) {
			throw new IllegalStateException(name + " not found in map for key: " + key);
		}
	}
}
