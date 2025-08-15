package com.ducnh.highperformance.concurrent.status;

import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import com.ducnh.highperformance.DirectBuffer;
import com.ducnh.highperformance.MutableDirectBuffer;
import com.ducnh.highperformance.concurrent.AtomicBuffer;
import com.ducnh.highperformance.concurrent.EpochClock;

public class ConcurrentCountersManager extends CountersManager{
	private final ReentrantLock lock = new ReentrantLock();
	
	public ConcurrentCountersManager(final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer) {
		super(metaDataBuffer, valuesBuffer);
	}
	
	public ConcurrentCountersManager(
		final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer, final Charset labelCharset) {
		super(metaDataBuffer, valuesBuffer, labelCharset);
	}
	
	public ConcurrentCountersManager(
		final AtomicBuffer metaDataBuffer,
		final AtomicBuffer valuesBuffer,
		final Charset labelCharset,
		final EpochClock epochClock,
		final long freeToReuseTimeoutMs) 
	{
		super(metaDataBuffer, valuesBuffer, labelCharset, epochClock, freeToReuseTimeoutMs);
	}
	
	public int available() {
		lock.lock();
		try {
			return super.available();
		} finally {
			lock.unlock();
		}
	}
	
	public int allocate(final String label, final int typeId) {
		lock.lock();
		try {
			return super.allocate(label, typeId);
		} finally {
			lock.unlock();
		}
	}
	
	public int allocate(final String label, final int typeId, final Consumer<MutableDirectBuffer> keyFunc) {
		lock.lock();
		try {
			return super.allocate(label, typeId, keyFunc);
		} finally {
			lock.unlock();
		}
	}
	
	public int allocate(
		final int typeId,
		final DirectBuffer keyBuffer,
		final int keyOffset,
		final int keyLength,
		final DirectBuffer labelBuffer,
		final int labelOffset,
		final int labelLength) 
	{
		lock.lock();
		try {
			return super.allocate(typeId, keyBuffer, keyOffset, keyLength, labelBuffer, labelOffset, labelLength);
		} finally {
			lock.unlock();
		}
	}
	
	public void free(final int counterId) {
		lock.lock();
		try {
			super.free(counterId);
		} finally {
			lock.unlock();
		}
	}
	
	public void setCounterValue(final int counterId, final long value) {
		lock.lock();
		try {
			super.setCounterValue(counterId, value);
		} finally {
			lock.unlock();
		}
	}
	
	public void setCounterRegistrationId(final int counterId, final long registrationId) {
		lock.lock();
		try {
			super.setCounterRegistrationId(counterId, registrationId);
		} finally {
			lock.unlock();
		}
	}
	
	public void setCounterOwnerId(final int counterId, final long ownerId) {
		lock.lock();
		try {
			super.setCounterOwnerId(counterId, ownerId);
		} finally {
			lock.unlock();
		}
	}
	
	public void setCounterReferenceId(final int counterId, final long referenceId) {
		lock.lock();
		try {
			super.setCounterReferenceId(counterId, referenceId);
		} finally {
			lock.unlock();
		}
	}
	
	public void setCounterLabel(final int counterId, final String label) {
		lock.lock();
		try {
			super.setCounterLabel(counterId, label);
		} finally {
			lock.unlock();
		}
	}
	
	public void setCounterKey(final int counterId, final DirectBuffer keyBuffer, final int offset, final int length) {
		lock.lock();
		try {
			super.setCounterKey(counterId, keyBuffer, offset, length);
		} finally {
			lock.unlock();
		}
	}
	
	public void appendToLabel(final int counterId, final String label) {
		lock.lock();
		try {
			super.appendToLabel(counterId, label);
		} finally {
			lock.unlock();
		}
	}
	
	public String toString() {
		lock.unlock();
		try {
			return super.toString();
		} finally {
			lock.unlock();
		}
	}
}
