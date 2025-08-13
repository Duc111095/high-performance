package com.ducnh.highperformance.concurrent;

import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("unused")
abstract class BackoffIdleStrategyPrePad {
    byte p000, p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    byte p016, p017, p018, p019, p020, p021, p022, p023, p024, p025, p026, p027, p028, p029, p030, p031;
    byte p032, p033, p034, p035, p036, p037, p038, p039, p040, p041, p042, p043, p044, p045, p046, p047;
    byte p048, p049, p050, p051, p052, p053, p054, p055, p056, p057, p058, p059, p060, p061, p062, p063;	
}

abstract class BackoffIdleStrategyData extends BackoffIdleStrategyPrePad {
	protected static final int NOT_IDLE = 0;
	protected static final int SPINNING = 1;
	protected static final int YIELDING = 2;
	protected static final int PARKING = 3;
	
	protected final long maxSpins;
	protected final long maxYields;
	protected final long minParkPeriodNs;
	protected final long maxParkPeriodNs;

	protected int state = NOT_IDLE;
	protected long spins;
	protected long yields;
	protected long parkPeriodNs;
	
	BackoffIdleStrategyData(
		final long maxSpins, final long maxYields, final long minParkPeriodNs, final long maxParkPeriodNs) {
		this.maxSpins = maxSpins;
		this.maxYields = maxYields;
		this.minParkPeriodNs = minParkPeriodNs;
		this.maxParkPeriodNs = maxParkPeriodNs;
	}
}

public final class BackoffIdleStrategy extends BackoffIdleStrategyData implements IdleStrategy{
	
	public static final String ALIAS = "backoff";
	public static final long DEFAULT_MAX_SPINS = 10L;
	public static final long DEFAULT_MAX_YIELDS = 5L;
	public static final long DEFAULT_MIN_PARK_PERIOD_NS = 1_000L;
	public static final long DEFAULT_MAX_PARK_PERIOD_NS = 1_000_000L;
	
    byte p064, p065, p066, p067, p068, p069, p070, p071, p072, p073, p074, p075, p076, p077, p078, p079;
    byte p080, p081, p082, p083, p084, p085, p086, p087, p088, p089, p090, p091, p092, p093, p094, p095;
    byte p096, p097, p098, p099, p100, p101, p102, p103, p104, p105, p106, p107, p108, p109, p110, p111;
    byte p112, p113, p114, p115, p116, p117, p118, p119, p120, p121, p122, p123, p124, p125, p126, p127;

    public BackoffIdleStrategy() {
    	super(DEFAULT_MAX_SPINS, DEFAULT_MAX_YIELDS, DEFAULT_MIN_PARK_PERIOD_NS, DEFAULT_MAX_PARK_PERIOD_NS);
    }
    
    public BackoffIdleStrategy(
    	final long maxSpins, final long maxYields, final long minParkPeriodNs, final long maxParkPeriodNs) {
    	super(maxSpins, maxYields, minParkPeriodNs, maxParkPeriodNs);
    }
    
    public void idle(final int workCount) {
    	if (workCount > 0) {
    		reset();
    	}
    	else {
    		idle();
    	}
    }
    
    public void idle() {
    	switch (state) {
    	case NOT_IDLE:
    		state = SPINNING;
    		spins++;
    		break;
    	
    	case SPINNING:
    		Thread.onSpinWait();
    		if (++spins > maxSpins) {
    			state =  YIELDING;
    			yields = 0;
    		}
    		break;
    	
    	case YIELDING:
    		if (++yields > maxYields) {
    			state = PARKING;
    			parkPeriodNs = minParkPeriodNs;
    		} else {
    			Thread.yield();
    		}
    		break;
    	
    	case PARKING:
    		LockSupport.parkNanos(parkPeriodNs);
    		parkPeriodNs = Math.min(parkPeriodNs << 1, maxParkPeriodNs);
    		break;
    	}
    }
    
    public void reset() {
    	spins = 0;
    	yields = 0;
    	parkPeriodNs = minParkPeriodNs;
    	state = NOT_IDLE;
    }
    
    public String alias() {
    	return ALIAS;
    }
    
    public String toString() {
    	return "BackoffIdleStrategy{" + 
    			"alias=" + ALIAS + 
    			", maxSpins=" + maxSpins + 
    			", maxYields=" + maxYields + 
    			", minParkPeriodNs=" + minParkPeriodNs + 
    			", maxParkPeriodNs=" + maxParkPeriodNs +
    			"}";
    }
}
