package com.ducnh.highperformance.concurrent.status;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import com.ducnh.highperformance.DirectBuffer;
import com.ducnh.highperformance.MutableDirectBuffer;
import com.ducnh.highperformance.UnsafeApi;
import com.ducnh.highperformance.concurrent.AtomicBuffer;

import static com.ducnh.highperformance.BitUtil.*;

public class AtomicCounter implements AutoCloseable{
	private boolean isClosed = false;
	private final int id;
	private final long addressOffset;
	private final byte[] byteArray;
	private CountersManager countersManager;
	
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	private final ByteBuffer byteBuffer;
	
	public AtomicCounter(final AtomicBuffer buffer, final int counterId) {
		this(buffer, counterId, null);
	}
	
	public AtomicCounter(final AtomicBuffer buffer, final int counterId, final CountersManager countersManager) {
		this.id = counterId;
		this.countersManager = countersManager;
		this.byteBuffer = buffer.byteBuffer();
		this.byteArray = buffer.byteArray();
		
		final int counterOffset = CountersManager.counterOffset(counterId);
		buffer.boundsCheck(counterOffset, SIZE_OF_LONG);
		this.addressOffset = buffer.addressOffset() + counterOffset;
	}
	
	public int id() {
		return id;
	}
	
	public void disconnectCountersManager() {
		countersManager = null;
	}
	
	public void close() {
		if (!isClosed) {
			isClosed = true;
			if (null != countersManager) {
				countersManager.free(id);
			}
		}
	}
	
	public boolean isClosed() {
		return isClosed;
	}
	
	public String label() {
		return null != countersManager ? countersManager.getCounterLabel(id) : null;
	}
	
	public void updateLabel(final String newLabel) {
		if (null != countersManager) {
			countersManager.setCounterLabel(id, newLabel);
		}
		else {
			throw new IllegalStateException("Not constructed with CountersManager");
		}
	}
	
	public AtomicCounter appendToLabel(final String suffix) {
		if (null != countersManager) {
			countersManager.appendToLabel(id, suffix);
		} else {
			throw new IllegalStateException("Not constructed with CountersManager");
		}
		
		return this;
	}
	
	public void updateKey(final Consumer<MutableDirectBuffer> keyFunc) {
		if (null != countersManager) {
			countersManager.setCounterKey(id, keyFunc);
		}
		else {
			throw new IllegalStateException("Not constructed with CountersManager");
		}
	}
	
	public void updateKey(final DirectBuffer keyBuffer, final int offset, final int length) {
		if (null != countersManager) {
			countersManager.setCounterKey(id, keyBuffer, offset, length);
		} else {
			throw new IllegalStateException("Not constructed with CountersManager");
		}
	}
	
	public long increment() {
		return UnsafeApi.getAndAddLong(byteArray, addressOffset, 1);
	}
	
	public long incrementOrdered() {
		return incrementRelease();
	}
	
	public long incrementRelease() {
		final byte[] array = byteArray;
		final long offset = addressOffset;
		final long currentValue = UnsafeApi.getLong(array, offset);
		UnsafeApi.putLongRelease(array, offset, currentValue + 1);
		return currentValue;
	}
	
	public long incrementOpaque() {
		final byte[] array = byteArray;
		final long offset = addressOffset;
		final long currentValue = UnsafeApi.getLong(array, offset);
		UnsafeApi.putLongOpaque(array, offset, currentValue + 1);
		return currentValue;
	}
	
	public long incrementPlain() {
		final byte[] array = byteArray;
		final long offset = addressOffset;
		final long currentValue = UnsafeApi.getLong(array, offset);
		UnsafeApi.putLong(array, offset, currentValue + 1);
		return currentValue;
	}
	
	public long decrement() {
		return UnsafeApi.getAndAddLong(byteArray, addressOffset, -1);
	}
	
	public long decrementOrdered() {
		return decrementRelease();
	}
	
	public long decrementRelease() {
		final byte[] array = byteArray;
		final long offset = addressOffset;
		final long currentValue = UnsafeApi.getLong(array, offset);
		UnsafeApi.putLongRelease(array, offset, currentValue - 1);
		
		return currentValue;
	}
	
	public long decrementOpaque() {
		final byte[] array = byteArray;
		final long offset = addressOffset;
		final long currentValue = UnsafeApi.getLong(array, offset);
		UnsafeApi.putLongOpaque(array, offset, currentValue - 1);
		
		return currentValue;
	}
	
	public long decrementPlain() {
		final byte[] array = byteArray;
		final long offset = addressOffset;
		final long currentValue = UnsafeApi.getLong(array, offset);
		UnsafeApi.putLong(array, offset, currentValue - 1);
		
		return currentValue;
	}
	
	public void set(final long value) {
		UnsafeApi.putLongVolatile(byteArray, addressOffset, value);
	}
	
	public void setOrdered(final long value) {
		setRelease(value);
	}
	
	public void setRelease(final long value) {
		UnsafeApi.putLongRelease(byteArray, addressOffset, value);
	}
	
	public void setOpaque(final long value) {
		UnsafeApi.putLongOpaque(byteArray, addressOffset, value);
	}
	
	public void setWeak(final long value) {
		setPlain(value);
	}
	
	public void setPlain(final long value) {
		UnsafeApi.putLong(byteArray, addressOffset, value);
	}
	
	public long getAndAdd(final long increment) {
		return UnsafeApi.getAndAddLong(byteArray, addressOffset, increment);
	}
	
	public long getAndAddOrdered(final long increment) {
		return getAndAddRelease(increment);
	}
	
	public long getAndAddRelease(final long increment) {
		final byte[] array = byteArray;
		final long offset = addressOffset;
		final long currentValue = UnsafeApi.getLong(array, offset);
		UnsafeApi.putLongRelease(array, offset, currentValue + increment);
		
		return currentValue;
	}
	
	public long getAndAddOpaque(final long increment) {
		final byte[] array = byteArray;
		final long offset = addressOffset;
		final long currentValue = UnsafeApi.getLong(array, offset);
		UnsafeApi.putLong(array, offset, currentValue + increment);
		
		return currentValue;
	}
	
	public long getAndAddPlain(final long increment) {
		final byte[] array = byteArray;
		final long offset = addressOffset;
		final long currentValue = UnsafeApi.getLong(array, offset);
		UnsafeApi.putLong(array, offset, currentValue + increment);
		return currentValue;
	}
	
	public long getAndSet(final long value) {
		return UnsafeApi.getAndSetLong(byteArray, addressOffset, value);
	}
	
	public boolean compareAndSet(final long expectedValue, final long updateValue) {
		return UnsafeApi.compareAndSetLong(byteArray, addressOffset, expectedValue, updateValue);
	}
	
	public long get() {
		return UnsafeApi.getLongVolatile(byteArray, addressOffset);
	}
	
	public long getAcquire() {
		return UnsafeApi.getLongAcquire(byteArray, addressOffset);
	}
	
	public long getOpaque() {
		return UnsafeApi.getLongOpaque(byteArray, addressOffset);
	}
	
	public long getWeak() {
		return getPlain();
	}
	
	public long getPlain() {
		return UnsafeApi.getLong(byteArray, addressOffset);
	}
	
	public boolean proposedMax(final long proposedValue) {
		boolean updated = false;
		
		final byte[] array = byteArray;
		final long offset = addressOffset;
		if (UnsafeApi.getLong(array, offset) < proposedValue) {
			UnsafeApi.putLong(array, offset, proposedValue);
			updated = true;
		}
		
		return updated;
	}
	
	public boolean proposedMaxOrdered(final long proposedValue) {
		return proposedMaxRelease(proposedValue);
	}
	
	public boolean proposedMaxRelease(final long proposedValue) {
		boolean updated = false;
		
		final byte[] array = byteArray;
		final long offset = addressOffset;
		if (UnsafeApi.getLong(array, offset) < proposedValue) {
			UnsafeApi.putLongRelease(array, offset, proposedValue);
			updated = true;
		}
		
		return updated;
	}
	
	public boolean proposedMaxOpaque(final long proposedValue) {
		boolean updated = false;
		
		final byte[] array = byteArray;
		final long offset = addressOffset;
		if (UnsafeApi.getLong(array, offset) < proposedValue) {
			UnsafeApi.putLongOpaque(array, offset, proposedValue);
			updated = true;
		}
		
		return updated;
	}
	
	public String toString() {
		return "AtomicCounter{" + 
				"isClosed=" + isClosed() +
				", id=" + id +
				", value=" + (isClosed() ? -1 : get()) +
				", countersManager=" + countersManager + 
				"}";
	}
}
