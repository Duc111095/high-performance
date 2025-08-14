package com.ducnh.highperformance.concurrent;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

@SuppressWarnings("unused")
abstract class CachedNanoClockPadding {
    byte p000, p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    byte p016, p017, p018, p019, p020, p021, p022, p023, p024, p025, p026, p027, p028, p029, p030, p031;
    byte p032, p033, p034, p035, p036, p037, p038, p039, p040, p041, p042, p043, p044, p045, p046, p047;
    byte p048, p049, p050, p051, p052, p053, p054, p055, p056, p057, p058, p059, p060, p061, p062, p063;	
}

abstract class CachedNanoClockValue extends CachedNanoClockPadding {
	protected volatile long timeNs;
}

public class CachedNanoClock extends CachedNanoClockValue implements NanoClock{
	private static final AtomicLongFieldUpdater<CachedNanoClockValue> FIELD_UPDATER = 
		AtomicLongFieldUpdater.newUpdater(CachedNanoClockValue.class, "timeNs");
	
	public CachedNanoClock() {
		
	}
	
	public long nanoTime() {
		return timeNs;
	}
	
	public void update(final long timeNs) {
		FIELD_UPDATER.lazySet(this, timeNs);
	}
	
	public void advance(final long nanos) {
		FIELD_UPDATER.lazySet(this, timeNs + nanos);
	}
}
