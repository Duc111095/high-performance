package com.ducnh.highperformance.concurrent;

import com.ducnh.highperformance.concurrent.status.StatusIndicatorReader;

public final class ControllableIdleStrategy implements IdleStrategy{
	public static final String ALIAS = "controllable";
	
	public static final int NOT_CONTROLLED = 0;
	public static final int NOOP = 1;
	public static final int BUSY_SPIN = 2;
	public static final int YIELD = 3;
	public static final int PARK = 4;
	private static final long PARK_PERIOD_NANOSECONDS = 1000;
	
	private final StatusIndicatorReader statusIndicator;
}
