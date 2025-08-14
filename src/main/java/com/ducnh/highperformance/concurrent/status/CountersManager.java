package com.ducnh.highperformance.concurrent.status;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.ducnh.highperformance.DirectBuffer;
import com.ducnh.highperformance.LangUtil;
import com.ducnh.highperformance.MutableDirectBuffer;
import com.ducnh.highperformance.collections.IntArrayList;
import com.ducnh.highperformance.concurrent.AtomicBuffer;
import com.ducnh.highperformance.concurrent.CachedEpochClock;
import com.ducnh.highperformance.concurrent.EpochClock;
import com.ducnh.highperformance.concurrent.UnsafeBuffer;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_INT;

public class CountersManager extends CountersReader{
	private final long freeToReuseTimeoutMs;
	private int highWaterMarkId = -1;
	private final IntArrayList freeList = new IntArrayList();
	private final EpochClock epochClock;
	
	public CountersManager(
		final AtomicBuffer metaDataBuffer,
		final AtomicBuffer valuesBuffer,
		final Charset labelCharset,
		final EpochClock epochClock,
		final long freeToReuseTimeoutMs) {
		super(metaDataBuffer, valuesBuffer, labelCharset);
		
		valuesBuffer.verifyAlignment();
		this.epochClock = epochClock;
		this.freeToReuseTimeoutMs = freeToReuseTimeoutMs;
		
		if (metaDataBuffer.capacity() < (valuesBuffer.capacity() * (METADATA_LENGTH / COUNTER_LENGTH))) {
			throw new IllegalArgumentException("metadata buffer is too small");
		}
	}
	
	public CountersManager(
		final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer, final Charset labelCharset) {
		this(metaDataBuffer, valuesBuffer, labelCharset, new CachedEpochClock(), 0);
	}
	
	public CountersManager(final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer) {
		this(metaDataBuffer, valuesBuffer, StandardCharsets.UTF_8, new CachedEpochClock(), 0);
	}
	
	public int capacity() {
		return maxCounterId + 1;
	}
	
	public int available() {
		int freeListCount = 0;
		
		if (!freeList.isEmpty()) {
			final long nowMs = epochClock.time();
			
			for (int i = 0, size = freeList.size(); i < size; i++) {
				final int counterId = freeList.getInt(i);
				if (nowMs >= metaDataBuffer.getLong(metaDataOffset(counterId) + FREE_FOR_REUSE_DEADLINE_OFFSET)) {
					freeListCount ++;
				}
			}
		}
		
		return (capacity() - highWaterMarkId - 1) + freeListCount;
	}
	
	public int allocate(final String label) {
		return allocate(label, DEFAULT_TYPE_ID);
	}
	
	public int allocate(final String label, final int typeId) {
		final int counterId = nextCounterId();
		final int recordOffset = metaDataOffset(counterId);
		
		try {
			metaDataBuffer.putInt(recordOffset + TYPE_ID_OFFSET, typeId);
			metaDataBuffer.putLong(recordOffset + FREE_FOR_REUSE_DEADLINE_OFFSET, NOT_FREE_TO_REUSE);
			putLabel(recordOffset, label);
			
			metaDataBuffer.putIntRelease(recordOffset, RECORD_ALLOCATED);
		} catch (final Exception ex) {
			freeList.pushInt(counterId);
			LangUtil.rethrowUnchecked(ex);
		}
		
		return counterId;
	}
	
	public int allocate(final String label, final int typeId, final Consumer<MutableDirectBuffer> keyFunc) {
		final int counterId = nextCounterId();
		
		try {
			final int recordOffset = metaDataOffset(counterId);
			
			metaDataBuffer.putInt(recordOffset + TYPE_ID_OFFSET, typeId);
			keyFunc.accept(new UnsafeBuffer(metaDataBuffer, recordOffset + KEY_OFFSET, MAX_KEY_LENGTH));
			metaDataBuffer.putLong(recordOffset + FREE_FOR_REUSE_DEADLINE_OFFSET, NOT_FREE_TO_REUSE);
			putLabel(recordOffset, label);
			
			metaDataBuffer.putIntRelease(recordOffset, RECORD_ALLOCATED);
		} catch (final Exception ex) {
			freeList.pushInt(counterId);
			LangUtil.rethrowUnchecked(ex);
		}
		
		return counterId;
	}
	
	public int allocate(
		final int typeId, 
		final DirectBuffer keyBuffer,
		final int keyOffset,
		final int keyLength, 
		final DirectBuffer labelBuffer,
		final int labelOffset,
		final int labelLength) {
		final int counterId = nextCounterId();
		
		try {
			final int recordOffset = metaDataOffset(counterId);
			
			metaDataBuffer.putInt(recordOffset + TYPE_ID_OFFSET, typeId);
			metaDataBuffer.putLong(recordOffset + FREE_FOR_REUSE_DEADLINE_OFFSET, NOT_FREE_TO_REUSE);
		
			if (null != keyBuffer) {
				final int length = Math.min(keyLength, MAX_KEY_LENGTH);
				metaDataBuffer.putBytes(recordOffset + KEY_OFFSET, keyBuffer, keyOffset, length);
			}
			
			final int length = Math.min(labelLength, MAX_LABEL_LENGTH);
			metaDataBuffer.putInt(recordOffset + LABEL_OFFSET, length);
			metaDataBuffer.putBytes(recordOffset + LABEL_OFFSET + SIZE_OF_INT, labelBuffer, labelOffset, length);
			
			metaDataBuffer.putIntRelease(recordOffset, RECORD_ALLOCATED);
		} catch (final Exception ex) {
			freeList.pushInt(counterId);
			LangUtil.rethrowUnchecked(ex);
		}
		
		return counterId;
	}
	
	public AtomicCounter newCounter(final String label) {
		return new AtomicCounter(valuesBuffer, allocate(label), this);
	}
	
	public AtomicCounter newCounter(final String label, final int typeId) {
		return new AtomicCounter(valuesBuffer, allocate(label, typeId), this);
	}
	
	public AtomicCounter newCounter(final String label, final int typeId, final Consumer<MutableDirectBuffer> keyFunc) {
		return new AtomicCounter(valuesBuffer, allocate(label, typeId, keyFunc), this);
	}
	
	public AtomicCounter newCounter (
		final int typeId,
		final DirectBuffer keyBuffer,
		final int keyOffset,
		final int keyLength,
		final DirectBuffer labelBuffer,
		final int labelOffset, 
		final int labelLength) 
	{
		return new AtomicCounter(
			valuesBuffer,
			allocate(typeId, keyBuffer, keyOffset, keyLength, labelBuffer, labelOffset, labelLength),
			this);
	}
	
	public void free(final int counterId) {
		validateCounterId(counterId);
		final int offset = metaDataOffset(counterId);
		
		if (RECORD_ALLOCATED != metaDataBuffer.getIntVolatile(offset)) {
			throw new IllegalStateException("counter not allocated: id=" + counterId);
		}
		
		metaDataBuffer.putIntRelease(offset, RECORD_RECLAIMED);
		metaDataBuffer.setMemory(offset + KEY_OFFSET, MAX_KEY_LENGTH, (byte)0);
		metaDataBuffer.putLong(offset + FREE_FOR_REUSE_DEADLINE_OFFSET, epochClock.time() + freeToReuseTimeoutMs);
		freeList.addInt(counterId);
	}
	
	public void setCounterValue(final int counterId, final long value) {
		validateCounterId(counterId);
		valuesBuffer.putLongRelease(counterOffset(counterId), value);
	}
	
	public void setCounterRegistrationId(final int counterId, final long registrationId) {
		validateCounterId(counterId);
		valuesBuffer.putLongRelease(counterOffset(counterId) + REGISTRATION_ID_OFFSET, registrationId);
	}
	
	public void setCounterOwnerId(final int counterId, final long ownerId) {
		validateCounterId(counterId);
		valuesBuffer.putLong(counterOffset(counterId) + OWNER_ID_OFFSET, ownerId);
	}
	
	public void setCounterReferenceId(final int counterId, final long referenceId) {
		validateCounterId(counterId);
		valuesBuffer.putLong(counterOffset(counterId) + REFERENCE_ID_OFFSET, referenceId);
	}
	
	public void setCounterLabel(final int counterId, final String label) {
		validateCounterId(counterId);
		putLabel(metaDataOffset(counterId), label);
	}
	
	public void setCounterKey(final int counterId, final Consumer<MutableDirectBuffer> keyFunc) {
		validateCounterId(counterId);
		keyFunc.accept(new UnsafeBuffer(metaDataBuffer, metaDataOffset(counterId) + KEY_OFFSET, MAX_KEY_LENGTH));
	}
	
	public void setCounterKey(final int counterId, final DirectBuffer keyBuffer, final int offset, final int length) {
		validateCounterId(counterId);
		if (length > MAX_KEY_LENGTH) {
			throw new IllegalArgumentException("key is too long: " + length + ", max: " + MAX_KEY_LENGTH); 
		}
		
		metaDataBuffer.putBytes(metaDataOffset(counterId) + KEY_OFFSET, keyBuffer, offset, length);
	}
	
	public void appendToLabel(final int counterId, final String label) {
		appendLabel(metaDataOffset(counterId), label);
	}
	
	public String toString() {
		return getClass().getSimpleName() + "{" + 
				"freeToReuseTimeoutMs=" + freeToReuseTimeoutMs + 
				", highWaterMarkId=" + highWaterMarkId + 
				", freeList=" + freeList +
				", epochClock=" + epochClock +
				"}";
	}
	
	private int nextCounterId() {
		if (!freeList.isEmpty()) {
			final long nowMs = epochClock.time();
			
			for (int i = 0, size = freeList.size(); i < size; i++) {
				final int counterId = freeList.getInt(i);
				
				if (nowMs >= metaDataBuffer.getLong(metaDataOffset(counterId) + FREE_FOR_REUSE_DEADLINE_OFFSET)) {
					freeList.remove(i);
					
					final int offset = counterOffset(counterId);
					valuesBuffer.putLongRelease(offset + REGISTRATION_ID_OFFSET, DEFAULT_REGISTRATION_ID);
					valuesBuffer.putLong(offset + OWNER_ID_OFFSET, DEFAULT_OWNER_ID);
					valuesBuffer.putLong(offset + REFERENCE_ID_OFFSET, DEFAULT_REFERENCE_ID);
					valuesBuffer.putLongRelease(offset, 0L);
					return counterId;
				}
			}
		}
		
		checkCountersCapacity(highWaterMarkId + 1);
		
		return ++highWaterMarkId;
	}
	
	private void putLabel(final int recordOffset, final String label) {
		if (StandardCharsets.US_ASCII == labelCharset) {
			final int length = metaDataBuffer.putStringWithoutLengthAscii(
				recordOffset + LABEL_OFFSET + SIZE_OF_INT, label, 0, MAX_LABEL_LENGTH);
			metaDataBuffer.putIntRelease(recordOffset + LABEL_OFFSET, length);
		} else {
			final byte[] bytes = label.getBytes(labelCharset);
			final int length = Math.min(bytes.length, MAX_LABEL_LENGTH);
			
			metaDataBuffer.putBytes(recordOffset + LABEL_OFFSET + SIZE_OF_INT, bytes, 0, length);
			metaDataBuffer.putIntRelease(recordOffset + LABEL_OFFSET, length);
		}
	}
	
	private void appendLabel(final int recordOffset, final String suffix) {
		final int existingLength = metaDataBuffer.getIntVolatile(recordOffset + LABEL_OFFSET);
		final int maxSuffixLength = MAX_LABEL_LENGTH - existingLength;
		
		if (StandardCharsets.US_ASCII == labelCharset) {
			final int suffixLength = metaDataBuffer.putStringWithoutLengthAscii(
				recordOffset + LABEL_OFFSET + SIZE_OF_INT + existingLength, suffix, 0, maxSuffixLength);
			
			metaDataBuffer.putIntRelease(recordOffset + LABEL_OFFSET, existingLength + suffixLength);
		} else {
			final byte[] suffixBytes = suffix.getBytes(labelCharset);
			final int suffixLength = Math.min(suffixBytes.length, maxSuffixLength);
			
			metaDataBuffer.putBytes(
				recordOffset + LABEL_OFFSET + SIZE_OF_INT + existingLength, suffixBytes, 0, suffixLength);
			metaDataBuffer.putIntRelease(recordOffset + LABEL_OFFSET, existingLength + suffixLength);
		}
	}
	
	private void checkCountersCapacity(final int counterId) {
		if (counterId > maxCounterId) {
			throw new IllegalStateException("unable to allocate counter, buffer is full: maxCounterId=" + maxCounterId);
		}
	}
}
