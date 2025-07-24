package com.ducnh.highperformance;

public final class Strings {
	private Strings() {}
	
	public static boolean isEmpty(final String value) {
		return value == null || value.isEmpty();
	}
	
	public static int parseIntOrDefault(final String value, final int defaultValue) throws NumberFormatException {
		if (value == null) {
			return defaultValue;
		}
		return Integer.parseInt(value);
	}
}
