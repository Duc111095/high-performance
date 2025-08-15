package com.ducnh.highperformance.concurrent.status;

import java.nio.ByteBuffer;

import com.ducnh.highperformance.UnsafeApi;
import com.ducnh.highperformance.concurrent.UnsafeBuffer;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_LONG;;

@SuppressWarnings("removal")
public class UnsafeBufferPosition extends Position{
	private boolean isClosed = false;
	private final int counterId;
	private final long addressOffset;
	private final byte[] byteArray;
	private final CountersManager countersManager;
	
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	private final ByteBuffer byteBuffer;
	
	public UnsafeBufferPosition(final UnsafeBuffer buffer, final int counterId) {
		this(buffer, counterId, null);
	}
	
	public UnsafeBufferPosition(final UnsafeBuffer buffer, final int counterId, final CountersManager countersManager) {
		this.counterId = counterId;
		this.countersManager = countersManager;
		this.byteArray = buffer.byteArray();
		this.byteBuffer = buffer.byteBuffer();
		
		final int counterOffset = CountersManager.counterOffset(counterId);
		buffer.boundsCheck(counterId, SIZE_OF_LONG);
		this.addressOffset = buffer.addressOffset() + counterOffset;
	}
	
	public boolean isClosed() {
		return isClosed;
	}
	
	public int id() {
		return counterId;
	}
	
	public long getVolatile() {
		return UnsafeApi.getLongVolatile(byteArray, addressOffset);
	}
	
	public long getAcquire() {
		return UnsafeApi.getLongAcquire(byteArray, addressOffset);
	}
	
	public long getOpaque() {
		return UnsafeApi.getLongOpaque(byteArray, addressOffset);
	}
	
	public long get() {
		return UnsafeApi.getLong(byteArray, addressOffset);
	}
	
	public void setVolatile(final long value) {
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
	
	public void set(final long value) {
		UnsafeApi.putLong(byteArray, addressOffset, value);
	}
	
	public boolean proposeMax(final long proposedValue) {
		boolean updated = false;
		
		final byte[] array = byteArray;
		final long offset = addressOffset;
		if (UnsafeApi.getLong(array, offset) < proposedValue) {
			UnsafeApi.putLong(array, offset, proposedValue);
			updated = true;
		}
		
		return updated;
	}
	
	public boolean proposeMaxOrdered(final long proposedValue) {
		return proposeMaxRelease(proposedValue);
	}
	
	public boolean proposeMaxRelease(final long proposedValue) {
		boolean updated = false;
		
		final byte[] array = byteArray;
		final long offset = addressOffset;
		if (UnsafeApi.getLong(array, offset) < proposedValue) {
			UnsafeApi.putLongRelease(array, offset, proposedValue);
			updated = true;
		}
		
		return updated;
	}
	
	public boolean proposeMaxOpaque(final long proposedValue) {
		boolean updated = false;
		
		final byte[] array = byteArray;
		final long offset = addressOffset;
		if (UnsafeApi.getLong(array, offset) < proposedValue) {
			UnsafeApi.putLongOpaque(array, offset, proposedValue);
			updated = true;
		}
		
		return updated;
	}
	
	public void close() {
		if (!isClosed) {
			isClosed = true;
			if (null != countersManager) {
				countersManager.free(counterId);
			}
		}
	}
	
	public String toString() {
		return "UnsafeBufferPosition{" + 
				"isClosed=" + isClosed() + 
				", counterId=" + counterId + 
				", value=" + (isClosed() ? -1 : getVolatile()) + 
				"}";
	}
}
