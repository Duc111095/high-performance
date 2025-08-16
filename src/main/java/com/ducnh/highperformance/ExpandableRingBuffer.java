package com.ducnh.highperformance;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_LONG;

import java.nio.ByteBuffer;

import com.ducnh.highperformance.collections.ArrayUtil;
import com.ducnh.highperformance.concurrent.UnsafeBuffer;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_INT;

public class ExpandableRingBuffer {

	public static final int MAX_CAPACITY = 1 << 30;
	public static final int HEADER_ALIGNMENT = SIZE_OF_LONG;
	public static final int HEADER_LENGTH = SIZE_OF_INT + SIZE_OF_INT;
	
	@FunctionalInterface
	public interface MessageConsumer{
		boolean onMessage(MutableDirectBuffer buffer, int offset, int length, int headOffset);
	}
	
	private static final int MESSAGE_LENGTH_OFFSET = 0;
	private static final int MESSAGE_TYPE_OFFSET = MESSAGE_LENGTH_OFFSET + SIZE_OF_INT;
	private static final int MESSAGE_TYPE_PADDING = 0;
	private static final int MESSAGE_TYPE_DATA = 1;
	
	private final int maxCapacity;
	private int capacity;
	private int mask;
	private long head;
	private long tail;
	private final UnsafeBuffer buffer = new UnsafeBuffer();
	private final boolean isDirect;
	
	public ExpandableRingBuffer() {
		this(0, MAX_CAPACITY, true);
	}
	
	public ExpandableRingBuffer(final int initialCapacity, final int maxCapacity, final boolean isDirect) {
		this.isDirect = isDirect;
		this.maxCapacity = maxCapacity;
		
		if (maxCapacity < 0 || maxCapacity > MAX_CAPACITY || !BitUtil.isPowerOfTwo(maxCapacity)) {
			throw new IllegalArgumentException("illegal max capacity: " + maxCapacity);
		}
		
		if (0 == initialCapacity) {
			buffer.wrap(ArrayUtil.EMPTY_BYTE_ARRAY);
			return;
		}
		
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("initial capacity < 0 : " + initialCapacity);
		}
		
		capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
		if (capacity < 0) {
			throw new IllegalArgumentException("invalid initial capacity: " + initialCapacity);
		}
		
		mask = capacity - 1;
		buffer.wrap(isDirect ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity));
	}
	
	public boolean isDirect() {
		return isDirect;
	}
	
	public int maxCapacity() {
		return maxCapacity;
	}
	
	public int capacity() {
		return capacity;
	}
	
	public int size() {
		return (int) (tail - head);
	}
	
	public boolean isEmpty() {
		return head == tail;
	}
	
	public long head() {
		return head;
	}
	
	public long tail() {
		return tail;
	}
	
	public void reset(final int requiredCapacity) {
		if (requiredCapacity < 0) {
			throw new IllegalArgumentException("required capacity <= 0: " + requiredCapacity);
		}
		
		final int newCapacity = BitUtil.findNextPositivePowerOfTwo(requiredCapacity);
		if (newCapacity < 0) {
			throw new IllegalArgumentException("invalid required capacity: " + requiredCapacity);
		}
		
		if (newCapacity > maxCapacity) {
			throw new IllegalArgumentException(
				"requiredCapacity=" + requiredCapacity + " > maxCapacity = " + maxCapacity);
		}
		
		if (newCapacity != capacity) {
			capacity = newCapacity;
			mask = newCapacity == 0 ? 0 : newCapacity - 1;
			buffer.wrap(isDirect ? ByteBuffer.allocateDirect(newCapacity) : ByteBuffer.allocate(newCapacity));
		}
		
		head = 0;
		tail = 0;
	}
	
	public int forEach(final MessageConsumer messageConsumer, final int limit) {
		long position = head;
		int count = 0;
		
		while (count < limit && position < tail) {
			final int offset = (int) position & mask;
			final int length = buffer.getInt(offset + MESSAGE_LENGTH_OFFSET);
			final int typeId = buffer.getInt(offset + MESSAGE_TYPE_OFFSET);
			final int alignedLength = BitUtil.align(length, HEADER_ALIGNMENT);
			position += alignedLength;
			
			if (MESSAGE_TYPE_PADDING != typeId) {
				final int headOffset = (int)(position - head);
				if (!messageConsumer.onMessage(buffer, offset + HEADER_LENGTH, length - HEADER_LENGTH, headOffset)) {
					break;
				}
				
				++count;
			}
		}
		
		return (int) (position - head);
	}
	
	public int forEach(final int headOffset, final MessageConsumer messageConsumer, final int limit) {
		if (headOffset < 0 || headOffset > size()) {
			throw new IllegalArgumentException("size=" + size() + " : headOffset=" + headOffset);
		}
		
		if (!BitUtil.isAligned(headOffset, HEADER_ALIGNMENT)) {
			throw new IllegalArgumentException(headOffset + " not aligned to " + HEADER_ALIGNMENT);
		}
		
		final long initialPosition = head + headOffset;
		long position = initialPosition;
		int count = 0;
		
		while (count < limit  && position < tail) {
			final int offset = (int) position & mask;
			final int length = buffer.getInt(offset + MESSAGE_LENGTH_OFFSET);
			final int typeId = buffer.getInt(offset + MESSAGE_TYPE_OFFSET);
			final int alignedLength = BitUtil.align(length, HEADER_ALIGNMENT);
			position += alignedLength;
			
			if (MESSAGE_TYPE_PADDING != typeId) {
				final int result = (int)(position - head);
				if (!messageConsumer.onMessage(buffer, offset + HEADER_LENGTH, length - HEADER_LENGTH, result)) 
				{
					break;
				}
				
				++count;
			}
		}
		
		return (int)(position - initialPosition);
	}
	
	public int consume(final MessageConsumer messageConsumer, final int messageLimit) {
		final int bytes;
		int count = 0;
		long position = head;
		
		try {
			while (count < messageLimit && position < tail) {
				final int offset = (int) position & mask;
				final int length = buffer.getInt(offset + MESSAGE_LENGTH_OFFSET);
				final int typeId = buffer.getInt(offset + MESSAGE_TYPE_OFFSET);
				final int alignedLength = BitUtil.align(length, HEADER_ALIGNMENT);
				
				position += alignedLength;
				
				if (MESSAGE_TYPE_PADDING != typeId) {
					final int headOffset = (int) (position - head);
					if (!messageConsumer.onMessage(buffer, offset + HEADER_LENGTH, length - HEADER_LENGTH, headOffset)) {
						position -= alignedLength;
						break;
					}
					
					++count;
				}
			}
		} finally {
			bytes = (int)(position - head);
			head = position;
		}
		
		return bytes;
	}
	
	public boolean append(final DirectBuffer srcBuffer, final int srcOffset, final int srcLength) {
		final int headOffset = (int) head & mask;
		final int tailOffset = (int) tail & mask;
		final int alignedLength = BitUtil.align(HEADER_LENGTH + srcLength, HEADER_ALIGNMENT);
		
		final int totalRemaining = capacity - (int)(tail - head);
		if (alignedLength > totalRemaining) {
			resize(alignedLength);
		} else if (tailOffset >= headOffset) {
			final int toEndRemaining = capacity - tailOffset;
			if (alignedLength > toEndRemaining) {
				if (alignedLength <= (totalRemaining - toEndRemaining)) {
					buffer.putInt(tailOffset + MESSAGE_LENGTH_OFFSET, toEndRemaining);
					buffer.putInt(tailOffset + MESSAGE_TYPE_OFFSET, MESSAGE_TYPE_PADDING);
					tail += toEndRemaining;
				} else {
					resize(alignedLength);
				}
			}
		}
		
		final int newTotalRemaining = capacity - (int)(tail - head);
		if (alignedLength > newTotalRemaining) {
			return false;
		}
		
		writeMessage(srcBuffer, srcOffset, srcLength);
		tail += alignedLength;
		return true;
	}
	
	private void resize(final int newMessageLength) {
		final int newCapacity = BitUtil.findNextPositivePowerOfTwo(capacity + newMessageLength);
		if (newCapacity < capacity || newCapacity > maxCapacity) {
			return;
		}
		
		final UnsafeBuffer tempBuffer = new UnsafeBuffer(
			isDirect ? ByteBuffer.allocateDirect(newCapacity) : ByteBuffer.allocate(newCapacity));
		
		final int headOffset = (int) head & mask;
		final int remaining = (int) (tail - head);
		final int firstCopyLength = Math.min(remaining, capacity - headOffset);
		tempBuffer.putBytes(0, buffer, headOffset, firstCopyLength);
		int tailOffset = firstCopyLength;
		
		if (firstCopyLength < remaining) {
			final int length = remaining - firstCopyLength;
			tempBuffer.putBytes(firstCopyLength, buffer, 0, length);
			tailOffset += length;
		}
		
		buffer.wrap(tempBuffer);
		capacity = newCapacity;
		mask = newCapacity - 1;
		head = 0;
		tail = tailOffset;
	}
	
	private void writeMessage(final DirectBuffer srcBuffer, final int srcOffset, final int srcLength) {
		final int offset = (int) tail & mask;
		
		buffer.putInt(offset + MESSAGE_LENGTH_OFFSET, HEADER_LENGTH + srcLength);
		buffer.putInt(offset + MESSAGE_TYPE_OFFSET, MESSAGE_TYPE_DATA);
		buffer.putBytes(offset + HEADER_LENGTH, srcBuffer, srcOffset, srcLength);
	}
}
 