package com.ducnh.highperformance.concurrent.status;

import java.nio.ByteBuffer;

import com.ducnh.highperformance.UnsafeApi;
import com.ducnh.highperformance.concurrent.AtomicBuffer;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_LONG;

public class UnsafeBufferStatusIndicator extends StatusIndicator{
	private final int counterId;
	private final long addressOffset;
	private final byte[] byteArray;
	
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	private final ByteBuffer byteBuffer;
	
	public UnsafeBufferStatusIndicator(final AtomicBuffer buffer, final int counterId) {
		this.counterId = counterId;
		this.byteArray = buffer.byteArray();
		this.byteBuffer = buffer.byteBuffer();
		
		final int counterOffset = CountersManager.counterOffset(counterId);
		buffer.boundsCheck(counterOffset, SIZE_OF_LONG);
		this.addressOffset = buffer.addressOffset() + counterOffset;
	}
	
	public int id() {
		return counterId;
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
	
	public long getVolatile() {
		return UnsafeApi.getLongVolatile(byteArray, addressOffset);
	}
	
	public long getAcquire() {
		return UnsafeApi.getLongAcquire(byteArray, addressOffset);
	}
	
	public long getOpaque() {
		return UnsafeApi.getLongOpaque(byteArray, addressOffset);
	}
	
	public String toString() {
		return "UnsafeBufferStatusIndicator{" +
				"counterId=" + counterId +
				", value=" + getVolatile() + 
				"}";
	}
}
