package com.ducnh.highperformance;

public final class LangUtil {
	private LangUtil() {}
	
	public static void rethrowUnchecked(final Throwable ex) {
		LangUtil.<RuntimeException>rethrow(ex);
	}
	
	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void rethrow(final Throwable t) throws T {
		throw (T)t;
	}
}
