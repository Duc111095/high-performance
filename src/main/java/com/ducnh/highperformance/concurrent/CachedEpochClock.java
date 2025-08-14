package com.ducnh.highperformance.concurrent;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

@SuppressWarnings("unused")
abstract class CachedEpochClockPadding {
    boolean p000, p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    boolean p016, p017, p018, p019, p020, p021, p022, p023, p024, p025, p026, p027, p028, p029, p030, p031;
    boolean p032, p033, p034, p035, p036, p037, p038, p039, p040, p041, p042, p043, p044, p045, p046, p047;
    boolean p048, p049, p050, p051, p052, p053, p054, p055, p056, p057, p058, p059, p060, p061, p062, p063; 
}

abstract class CachedEpochClockValue extends CachedEpochClockPadding {
	protected volatile long timeMs;
}

public class CachedEpochClock extends CachedEpochClockValue implements EpochClock{
	private static final AtomicLongFieldUpdater<CachedEpochClockValue> FIELD_UPDATER = 
		AtomicLongFieldUpdater.newUpdater(CachedEpochClockValue.class, "timeMs");
	
    boolean p064, p065, p066, p067, p068, p069, p070, p071, p072, p073, p074, p075, p076, p077, p078, p079;
    boolean p080, p081, p082, p083, p084, p085, p086, p087, p088, p089, p090, p091, p092, p093, p094, p095;
    boolean p096, p097, p098, p099, p100, p101, p102, p103, p104, p105, p106, p107, p108, p109, p110, p111;
    boolean p112, p113, p114, p115, p116, p117, p118, p119, p120, p121, p122, p123, p124, p125, p126, p127;

    public CachedEpochClock() {
    	
    }
    
    public long time() {
    	return timeMs;
    }
    
    public void update(final long timeMs) {
    	FIELD_UPDATER.lazySet(this, timeMs);
    }
    
    public void advance(final long millis) {
    	FIELD_UPDATER.lazySet(this, timeMs + millis);
    }
}
