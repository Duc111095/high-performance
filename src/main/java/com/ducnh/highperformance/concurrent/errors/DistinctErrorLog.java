package com.ducnh.highperformance.concurrent.errors;

import static com.ducnh.highperformance.BitUtil.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Objects;

import com.ducnh.highperformance.concurrent.AtomicBuffer;
import com.ducnh.highperformance.concurrent.EpochClock;

public final class DistinctErrorLog {
	public static final int LENGTH_OFFSET = 0;
	
	public static final int OBSERVATION_COUNT_OFFSET = SIZE_OF_INT;
	
	public static final int LAST_OBSERVATION_TIMESTAMP_OFFSET = OBSERVATION_COUNT_OFFSET + SIZE_OF_INT;
	
	public static final int FIRST_OBSERVATION_TIMESTAMP_OFFSET = LAST_OBSERVATION_TIMESTAMP_OFFSET + SIZE_OF_LONG;
	
	public static final int ENCODED_ERROR_OFFSET = FIRST_OBSERVATION_TIMESTAMP_OFFSET + SIZE_OF_LONG;
	
	public static final int RECORD_ALIGNMENT = SIZE_OF_LONG;
	
	static final DistinctObservation INSUFFICIENT_SPACE = new DistinctObservation(null, 0);
	
	private final EpochClock clock;
	private final AtomicBuffer buffer;
	private final Charset charset;
	private DistinctObservation[] distinctObservations = new DistinctObservation[0];
	int nextOffset = 0;
	
	public DistinctErrorLog(final AtomicBuffer buffer, final EpochClock clock) {
		this(buffer, clock, UTF_8);
	}
	
	public DistinctErrorLog(final AtomicBuffer buffer, final EpochClock clock, final Charset charset) {
		buffer.verifyAlignment();
		this.buffer = buffer;
		this.clock = clock;
		this.charset = charset;
	}
	
	public AtomicBuffer buffer() {
		return buffer;
	}
	
	public Charset charset() {
		return charset;
	}
	
	public boolean record(final Throwable exception) {
		final long timestampMs;
		DistinctObservation distinctObservation;
		
		timestampMs = clock.time();
		synchronized(this) {
			distinctObservation = find(distinctObservations, exception);
			
			if (null == distinctObservation) {
				distinctObservation = newObservation(timestampMs, exception);
				if (INSUFFICIENT_SPACE == distinctObservation) {
					return false;
				}
			}
		}
		
		final int offset = distinctObservation.offset;
		buffer.getAndAddInt(offset + OBSERVATION_COUNT_OFFSET, 1);
		buffer.putLongRelease(offset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestampMs);
		
		return true;
	}
	
	private static DistinctObservation find(final DistinctObservation[] existingObservations, final Throwable exception) {
		DistinctObservation existingObservation = null;
	
		for (final DistinctObservation o : existingObservations) {
			if (equals(o.throwable, exception)) {
				existingObservation = o;
				break;
			}
		}
		
		return existingObservation;
	}
	
	@SuppressWarnings("FinalParameters")
	private static boolean equals(Throwable lhs, Throwable rhs) {
		while(true) {
			if (lhs == rhs) {
				return true;
			}
			
			if (lhs.getClass() == rhs.getClass() && 
				Objects.equals(lhs.getMessage(), rhs.getMessage()) &&
				equals(lhs.getStackTrace(), rhs.getStackTrace())) {
				lhs = lhs.getCause();
				rhs = rhs.getCause();
				
				if (null == lhs && null == rhs) {
					return true;
				} else if (null != lhs && null != rhs) {
					continue;
				}
			} 
			
			return false;
		}
	}
	
	private static boolean equals(final StackTraceElement[] lhsStackTrace, final StackTraceElement[] rhsStackTrace) {
		if (lhsStackTrace.length != rhsStackTrace.length) {
			return false;
		}
		
		for (int i = 0, length = lhsStackTrace.length; i < length; i++) {
			final StackTraceElement lhs = lhsStackTrace[i];
			final StackTraceElement rhs = rhsStackTrace[i];
			
			if (lhs.getLineNumber() != rhs.getLineNumber() ||
				!lhs.getClassName().equals(rhs.getClassName()) || 
				!Objects.equals(lhs.getMethodName(), rhs.getMethodName()) ||
				!Objects.equals(lhs.getFileName(), rhs.getFileName()))
			{
				return false;
			}
		} 
		
		return true;
	}
	
	DistinctObservation newObservation(final long timestampMs, final Throwable exception) {
		final int offset = nextOffset;
		if (offset < 0) {
			return INSUFFICIENT_SPACE;
		}
		
		final int remainingCapacity = buffer.capacity() - ENCODED_ERROR_OFFSET - offset;
		if (remainingCapacity <= 0) {
			return INSUFFICIENT_SPACE;
		}
		
		final byte[] encodedError = encodedError(exception);
		if (remainingCapacity - encodedError.length < 0) {
			return INSUFFICIENT_SPACE;
		}
		
		final int length =  ENCODED_ERROR_OFFSET + encodedError.length;
		buffer.putBytes(offset + ENCODED_ERROR_OFFSET, encodedError);
		buffer.putLong(offset + FIRST_OBSERVATION_TIMESTAMP_OFFSET, timestampMs);
		nextOffset = align(offset + length, RECORD_ALIGNMENT);
		
		final DistinctObservation distinctObservation = new DistinctObservation(exception, offset);
		distinctObservations = prepend(distinctObservations, distinctObservation);
		buffer.putIntRelease(offset + LENGTH_OFFSET, length);
		
		return distinctObservation;
	}
	
	byte[] encodedError(final Throwable observation) {
		final StringWriter stringWriter = new StringWriter();
		observation.printStackTrace(new PrintWriter(stringWriter));
		return stringWriter.toString().getBytes(charset);
	}
	
	private static DistinctObservation[] prepend(
		final DistinctObservation[] observations, final DistinctObservation observation) {
		final int length = observations.length;
		final DistinctObservation[] newObservations = new DistinctObservation[length + 1];
		
		newObservations[0] = observation;
		System.arraycopy(observations, 0, newObservations, 1, length);
		
		return newObservations;
	}
	
	record DistinctObservation(Throwable throwable, int offset) {
		
	}
}
