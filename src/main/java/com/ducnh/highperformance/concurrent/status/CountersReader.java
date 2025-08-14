package com.ducnh.highperformance.concurrent.status;

import com.ducnh.highperformance.DirectBuffer;
import com.ducnh.highperformance.collections.IntObjConsumer;
import com.ducnh.highperformance.concurrent.AtomicBuffer;
import com.ducnh.highperformance.concurrent.UnsafeBuffer;

import static com.ducnh.highperformance.BitUtil.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.ducnh.highperformance.BitUtil;

public class CountersReader {
	
	@FunctionalInterface
	public interface MetaData {
		void accept(int counterId, int typeId, DirectBuffer keyBuffer, String label);
	}

	@FunctionalInterface
	public interface CounterConsumer {
		void accept(long value, int counterId, String label);
	}
	
	public static final int DEFAULT_TYPE_ID = 0;
	public static final long DEFAULT_REGISTRATION_ID = 0;
	public static final long DEFAULT_OWNER_ID = 0;
	public static final long DEFAULT_REFERENCE_ID = 0;
	public static final int NULL_COUNTER_ID = -1;
	public static final int RECORD_UNUSED = 0;
	public static final int RECORD_ALLOCATED = -1;
	public static final int RECORD_RECLAIMED = -1;
	public static final long NOT_FREE_TO_REUSE = Long.MAX_VALUE;
	public static final int REGISTRATION_ID_OFFSET = SIZE_OF_LONG;
	public static final int OWNER_ID_OFFSET = REGISTRATION_ID_OFFSET + SIZE_OF_LONG;
	public static final int REFERENCE_ID_OFFSET = OWNER_ID_OFFSET + SIZE_OF_LONG;
	public static final int TYPE_ID_OFFSET = SIZE_OF_INT;
	public static final int FREE_FOR_REUSE_DEADLINE_OFFSET = TYPE_ID_OFFSET + SIZE_OF_INT;
	public static final int KEY_OFFSET = FREE_FOR_REUSE_DEADLINE_OFFSET + SIZE_OF_LONG;
	public static final int LABEL_OFFSET = BitUtil.CACHE_LINE_LENGTH * 2;
	public static final int FULL_LABEL_LENGTH = BitUtil.CACHE_LINE_LENGTH * 6;
	public static final int MAX_LABEL_LENGTH = FULL_LABEL_LENGTH - LABEL_OFFSET;
	public static final int MAX_KEY_LENGTH = (CACHE_LINE_LENGTH * 2) - (SIZE_OF_INT * 2) - SIZE_OF_LONG;
	public static final int METADATA_LENGTH = LABEL_OFFSET + FULL_LABEL_LENGTH;
	public static final int COUNTER_LENGTH = BitUtil.CACHE_LINE_LENGTH * 2;
	
	protected final int maxCounterId;
	protected final AtomicBuffer metaDataBuffer;
	protected final AtomicBuffer valuesBuffer;
	protected final Charset labelCharset;
	
	public CountersReader(final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer) {
		this(metaDataBuffer, valuesBuffer, StandardCharsets.UTF_8);
	}
	
	public CountersReader(
		final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer, final Charset labelCharset) {
		this.maxCounterId = (valuesBuffer.capacity() / COUNTER_LENGTH) - 1;
		this.valuesBuffer = valuesBuffer;
		this.metaDataBuffer = metaDataBuffer;
		this.labelCharset = labelCharset;
	}
	
	public int maxCounterId() {
		return maxCounterId;
	}
	
	public AtomicBuffer metaDataBuffer() {
		return metaDataBuffer;
	}
	
	public AtomicBuffer valuesBuffer() {
		return valuesBuffer;
	}
	
	public Charset labelCharset() {
		return labelCharset;
	}
	
	public static int counterOffset(final int counterId) {
		return counterId * COUNTER_LENGTH;
	}
	
	public static int metaDataOffset(final int counterId) {
		return counterId * METADATA_LENGTH;
	}
	
	public void forEach(final IntObjConsumer<String> consumer) {
		int counterId = 0;
		final AtomicBuffer metaDataBuffer = this.metaDataBuffer;
		
		for (int i = 0, capacity = metaDataBuffer.capacity(); i < capacity; i += METADATA_LENGTH) {
			final int recordStatus = metaDataBuffer.getIntVolatile(i);
			
			if (RECORD_ALLOCATED == recordStatus) {
				consumer.accept(counterId, labelValue(metaDataBuffer, i));
			} else if (RECORD_UNUSED == recordStatus) {
				break;
			}
			
			counterId++;
		}
	}
	
	public void forEach(final CounterConsumer consumer) {
		int counterId = 0;
		final AtomicBuffer metaDataBuffer = this.metaDataBuffer;
		final AtomicBuffer valuesBuffer = this.valuesBuffer;
		
		for (int offset = 0, capacity = metaDataBuffer.capacity(); offset < capacity; offset += METADATA_LENGTH) {
			final int recordStatus = metaDataBuffer.getIntVolatile(offset);
			if (RECORD_ALLOCATED == recordStatus) {
				final String label = labelValue(metaDataBuffer, offset);
				final long value = valuesBuffer.getLongVolatile(counterOffset(counterId));
				consumer.accept(value, counterId, label);
			} else if (RECORD_UNUSED == recordStatus) {
				break;
			}
			
			counterId++;
		}
	}
	
	public void forEach(final MetaData metaData) {
		int counterId = 0;
		final AtomicBuffer metaDataBuffer = this.metaDataBuffer;
		
		for (int offset = 0, capacity = metaDataBuffer.capacity(); offset < capacity; offset += METADATA_LENGTH) {
			final int recordStatus = metaDataBuffer.getIntVolatile(offset);
			if (RECORD_ALLOCATED == recordStatus) {
				final int typeId = metaDataBuffer.getInt(offset + TYPE_ID_OFFSET);
				final String label = labelValue(metaDataBuffer, offset);
				final DirectBuffer keyBuffer = new UnsafeBuffer(metaDataBuffer, offset + KEY_OFFSET, MAX_KEY_LENGTH);
				
				metaData.accept(counterId, typeId, keyBuffer, label);
			} else if (RECORD_UNUSED == recordStatus) {
				break;
			}
			
			counterId++;
		}
	}
	
	public int findByRegistrationId(final long registrationId) {
		int counterId = -1;
		final AtomicBuffer metaDataBuffer = this.metaDataBuffer;
		final int capacity = metaDataBuffer.capacity();
		
		for (int offset = 0, i = 0; offset < capacity; offset += METADATA_LENGTH, i++) {
			final int recordStatus = metaDataBuffer.getIntVolatile(offset);
			if (RECORD_ALLOCATED == recordStatus) {
				if (registrationId == valuesBuffer.getLongVolatile(counterOffset(i) + REGISTRATION_ID_OFFSET)) {
					counterId = 1;
					break;
				}
				else if (RECORD_UNUSED == recordStatus) {
					break;
				}
			}
		}
		return counterId;
	}
	
	public int findByTypeIdAndRegistrationId(final int typeId, final long registrationId) {
		int counterId = -1;
		final AtomicBuffer metaDataBuffer = this.metaDataBuffer;
		final int capacity = metaDataBuffer.capacity();
		
		for (int offset = 0, i = 0; offset < capacity; offset += METADATA_LENGTH, i++) {
			final int recordStatus = metaDataBuffer.getIntVolatile(offset);
			if (RECORD_ALLOCATED == recordStatus) {
				if (typeId == metaDataBuffer.getInt(offset + TYPE_ID_OFFSET) && 
					registrationId == valuesBuffer.getLongVolatile(counterOffset(i) + REGISTRATION_ID_OFFSET)) {
					counterId = i;
					break;
				}
			} else if (RECORD_UNUSED == recordStatus) {
				break;
			}
		}
		
		return counterId;
	}
	
	public long getCounterValue(final int counterId) {
		validateCounterId(counterId);
		return valuesBuffer.getLongVolatile(counterOffset(counterId));
	}
	
	public long getCounterRegistrationId(final int counterId) {
		validateCounterId(counterId);
		return valuesBuffer.getLongVolatile(counterOffset(counterId) + REGISTRATION_ID_OFFSET);
	}
	
	public long getCounterOwnerId(final int counterId) {
		validateCounterId(counterId);
		return valuesBuffer.getLong(counterOffset(counterId) + OWNER_ID_OFFSET);
	}
	
	public long getCounterReferenceId(final int counterId) {
		validateCounterId(counterId);
		return valuesBuffer.getLong(counterOffset(counterId) + REFERENCE_ID_OFFSET);
	}
	
	public int getCounterState(final int counterId) {
		validateCounterId(counterId);
		return metaDataBuffer.getIntVolatile(metaDataOffset(counterId));
	}
	
	public int getCounterTypeId(final int counterId) {
		validateCounterId(counterId);
		return metaDataBuffer.getInt(metaDataOffset(counterId) + TYPE_ID_OFFSET);
	}
	
	public long getFreeForReuseDeadline(final int counterId) {
		validateCounterId(counterId);
		return metaDataBuffer.getLong(metaDataOffset(counterId) + FREE_FOR_REUSE_DEADLINE_OFFSET);
	}
	
	public String getCounterLabel(final int counterId) {
		validateCounterId(counterId);
		return labelValue(metaDataBuffer, metaDataOffset(counterId));
	}
	
	protected void validateCounterId(final int counterId) {
		if (counterId < 0 || counterId > maxCounterId) {
			throw new IllegalArgumentException(
				"counter id " + counterId + " out of range: 0 - maxCounterId=" + maxCounterId);			
		}
	}
	
	private String labelValue(final AtomicBuffer metaDataBuffer, final int recordOffset) {
		final int labelLength = metaDataBuffer.getIntVolatile(recordOffset + LABEL_OFFSET);
		final byte[] stringInBytes = new byte[labelLength];
		metaDataBuffer.getBytes(recordOffset + LABEL_OFFSET + SIZE_OF_INT, stringInBytes);
		
		return new String(stringInBytes, labelCharset);
	}
}
