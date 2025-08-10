package com.ducnh.highperformance.concurrent;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class OffsetEpochNanoClock implements EpochNanoClock {
	private static final int DEFAULT_MAX_MEASUREMENT_RETRIES = 100;
	private static final long DEFAULT_MEASUREMENT_THRESHOLD_NS = 250;
	private static final long DEFAULT_RESAMPLE_INTERVAL_NS = HOURS.toNanos(1);

	private final int maxMeasurementRetries;
	private final long measurementThresholdNs;
	private final long resampleIntervalNs;
	
	private volatile TimeFields timeFields;
	
	public OffsetEpochNanoClock() {
		this(DEFAULT_MAX_MEASUREMENT_RETRIES, DEFAULT_MEASUREMENT_THRESHOLD_NS, DEFAULT_RESAMPLE_INTERVAL_NS);
	}
	
	@SuppressWarnings("this-escape")
	public OffsetEpochNanoClock(
		final int maxMeasurementRetries, final long measurementThresholdNs, final long resampleIntervalNs) {
		this.maxMeasurementRetries = maxMeasurementRetries;
		this.measurementThresholdNs = measurementThresholdNs;
		this.resampleIntervalNs = resampleIntervalNs;
		
		sample();
	}
	
	public void sample() {
		long bestInitialCurrentNanoTime = 0, bestInitialNanoTime = 0;
		long bestNanoTimeWindow = Long.MAX_VALUE;
		
		final int maxMeasurementRetries = this.maxMeasurementRetries;
		final long measurementThresholdNs = this.measurementThresholdNs;
		
		for (int i = 0; i < maxMeasurementRetries; i++) {
			final long firstNanoTime = System.nanoTime();
			final long initialCurrentTimeMillis = System.currentTimeMillis();
			final long secondNanoTime = System.nanoTime();
			
			final long nanoTimeWindow = secondNanoTime - firstNanoTime;
			if (nanoTimeWindow < measurementThresholdNs) {
				timeFields = new TimeFields(
						MILLISECONDS.toNanos(initialCurrentTimeMillis),
						(firstNanoTime + secondNanoTime) >> 1,
						true);
				return;
			}
			else if (nanoTimeWindow < bestNanoTimeWindow) {
				bestInitialCurrentNanoTime = MILLISECONDS.toNanos(initialCurrentTimeMillis);
				bestInitialNanoTime = (firstNanoTime + secondNanoTime) >> 1;
				bestNanoTimeWindow = nanoTimeWindow;
			}
		}
		
		timeFields = new TimeFields(
				bestInitialCurrentNanoTime,
				bestInitialNanoTime,
				false);
	}
	
	public long nanoTime() {
		final TimeFields timeFields = this.timeFields;
		final long nanoTimeAdjustment = System.nanoTime() - timeFields.initialNanoTime;
		if (nanoTimeAdjustment < 0 || nanoTimeAdjustment > resampleIntervalNs) {
			sample();
			return nanoTime();
		}
		
		return timeFields.initialCurrentNanoTime + nanoTimeAdjustment;
	}
	
	public boolean isWithinThreshold() {
		return timeFields.isWithinThreshold;
	}
	
	static final class TimeFields {
		final long initialCurrentNanoTime;
		final long initialNanoTime;
		final boolean isWithinThreshold;
		
		private TimeFields(
			final long initialCurrentNanoTime, final long initialNanoTime, final boolean isWithinThreshold) {
			this.initialCurrentNanoTime = initialCurrentNanoTime;
			this.initialNanoTime = initialNanoTime;
			this.isWithinThreshold = isWithinThreshold;
		}
	}
}
