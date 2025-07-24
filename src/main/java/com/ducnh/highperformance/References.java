package com.ducnh.highperformance;

import java.lang.ref.Reference;

public final class References {
	private References() {}
	
	public static boolean isCleared(final Reference<?> ref) {
		return ref.get() == null;
	}
}
