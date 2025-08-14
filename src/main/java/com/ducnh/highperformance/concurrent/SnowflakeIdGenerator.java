package com.ducnh.highperformance.concurrent;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

abstract class AbstractSnowflakeIdGeneratorPaddingLhs {
    byte p000, p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    byte p016, p017, p018, p019, p020, p021, p022, p023, p024, p025, p026, p027, p028, p029, p030, p031;
    byte p032, p033, p034, p035, p036, p037, p038, p039, p040, p041, p042, p043, p044, p045, p046, p047;
    byte p048, p049, p050, p051, p052, p053, p054, p055, p056, p057, p058, p059, p060, p061, p062, p063;	
}

abstract class AbstractSnowflakeIdGeneratorValue extends AbstractSnowflakeIdGeneratorPaddingLhs {
	static final AtomicLongFieldUpdater<AbstractSnowflakeIdGeneratorValue> TIMESTAMP_SEQUENCE_UPDATER = 
			AtomicLongFieldUpdater.newUpdater(AbstractSnowflakeIdGeneratorValue.class, "timestampSequence");
	
	volatile long timestampSequence;
}

abstract class AbstractSnowflakeIdGeneratorPaddingRhs extends AbstractSnowflakeIdGeneratorValue {
    byte p064, p065, p066, p067, p068, p069, p070, p071, p072, p073, p074, p075, p076, p077, p078, p079;
    byte p080, p081, p082, p083, p084, p085, p086, p087, p088, p089, p090, p091, p092, p093, p094, p095;
    byte p096, p097, p098, p099, p100, p101, p102, p103, p104, p105, p106, p107, p108, p109, p110, p111;
    byte p112, p113, p114, p115, p116, p117, p118, p119, p120, p121, p122, p123, p124, p125, p126, p127;	
}

public final class SnowflakeIdGenerator extends AbstractSnowflakeIdGeneratorPaddingRhs implements IdGenerator {

	public static final int UNUSED_BITS = 1;
	public static final int EPOCH_BITS = 41;
	public static final int MAX_NODE_ID_AND_SEQUENCE_BITS = 22;
	public static final int NODE_ID_BITS_DEFAULT = 10;
	public static final int SEQUENCE_BITS_DEFAULT = 12;
	
	private final int nodeIdAndSequenceBits;
	private final int sequenceBits;
	private final long maxNodeId;
	private final long maxSequence;
	private final long nodeBits;
	private final long timestampOffsetMs;
	private final EpochClock clock;
	
	public SnowflakeIdGenerator (
		final int nodeIdBits,
		final int sequenceBits,
		final long nodeId,
		final long timestampOffsetMs,
		final EpochClock clock) {
		if (nodeIdBits < 0) {
			throw new IllegalArgumentException("must be >= 0: nodeIdBits=" + nodeIdBits);
		}
		if (sequenceBits < 0) {
			throw new IllegalArgumentException("must be >= 0: sequenceBits=" + sequenceBits);
		}
		
		final int nodeIdAndSequenceBits = (nodeIdBits + sequenceBits);
		if (nodeIdAndSequenceBits > MAX_NODE_ID_AND_SEQUENCE_BITS) {
			throw new IllegalArgumentException("too many bits used:" +
				" nodeIdBits=" + nodeIdBits + " + sequenceBits=" + sequenceBits +
				" > " + MAX_NODE_ID_AND_SEQUENCE_BITS);
		}
		
		final long maxNodeId = (long)(Math.pow(2, nodeIdBits) - 1);
		if (nodeId < 0 || nodeId > maxNodeId) {
			throw new IllegalArgumentException("must be >= 0 && <= " + maxNodeId + ": nodeId=" + nodeId);
		}
		
		if (timestampOffsetMs < 0) {
			throw new IllegalArgumentException("must be >= 0: timestampOffsetMs=" + timestampOffsetMs);
		}
		
		final long nowMs = clock.time();
		if (timestampOffsetMs > nowMs) {
			throw new IllegalArgumentException("timestampOffsetMs=" + timestampOffsetMs + " > nowMs=" + nowMs);
		}
		
		this.nodeIdAndSequenceBits = nodeIdAndSequenceBits;
		this.maxNodeId = maxNodeId;
		this.sequenceBits = sequenceBits;
		this.maxSequence = (long)(Math.pow(2, sequenceBits) - 1);
		this.nodeBits = nodeId << sequenceBits;
		this.timestampOffsetMs = timestampOffsetMs;
		this.clock = clock;
	}
	
	public SnowflakeIdGenerator(final long nodeId) {
		this(NODE_ID_BITS_DEFAULT, SEQUENCE_BITS_DEFAULT, nodeId, 0, SystemEpochClock.INSTANCE);
	}
	
	public long nodeId() {
		return nodeBits >>> sequenceBits;
	}
	
	public long timestampOffsetMs() {
		return timestampOffsetMs;
	}
	
	public long maxNodeId() {
		return maxNodeId;
	}
	
	public long maxSequence() {
		return maxSequence;
	}
	
	public long nextId() {
		while (true) {
			final long oldTimestampSequence = timestampSequence;
			final long timestampMs = clock.time() - timestampOffsetMs;
			final long oldTimestampMs = oldTimestampSequence >>> nodeIdAndSequenceBits;
			
			if (timestampMs > oldTimestampMs) {
				final long newTimestampSequence = timestampMs << nodeIdAndSequenceBits;
				if (TIMESTAMP_SEQUENCE_UPDATER.compareAndSet(this, oldTimestampSequence, newTimestampSequence)) {
					return newTimestampSequence | nodeBits;
				}
			}
			else {
				final long oldSequence = oldTimestampSequence & maxSequence;
				if (oldSequence < maxSequence) {
					final long newTimestampSequence = oldTimestampSequence + 1;
					if (TIMESTAMP_SEQUENCE_UPDATER.compareAndSet(this, oldTimestampSequence, newTimestampSequence)) {
						return newTimestampSequence | nodeBits;
					}
				}
			}
			
			if (Thread.currentThread().isInterrupted()) {
				throw new IllegalStateException("unexcepted thread interrupt");
			}
			
			Thread.onSpinWait();
		}
	}
	
	long extractTimestamp(final long id) {
		return id >>> nodeIdAndSequenceBits;
	}
	
	long extractNodeId(final long id) {
		return (id >>> sequenceBits) & maxNodeId;
	}
	
	long extractSequence(final long id) {
		return id & maxSequence;
	}
}
