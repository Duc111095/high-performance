package com.ducnh.highperformance.concurrent;

import java.util.Collection;
import java.util.function.Consumer;

import com.ducnh.highperformance.UnsafeApi;

@SuppressWarnings("removal")
public class ManyToManyConcurrentArrayQueue<E> extends AbstractConcurrentArrayQueue<E>{
	private static final int SEQUENCES_ARRAY_BASE = UnsafeApi.arrayBaseOffset(long[].class);
	
	private final long[] sequences;
	
	public ManyToManyConcurrentArrayQueue(final int requestedCapacity) {
		super(requestedCapacity);
		
		if (requestedCapacity < 2) {
			throw new IllegalArgumentException(
					"requestedCapacity must be >= 2: requestedCapacity=" + requestedCapacity);
		}
		
		final long[] sequences = new long[capacity];
		
		for (int i = 0; i < capacity; i++) {
			sequences[i] = i;
		}
		
		UnsafeApi.putLongVolatile(sequences, sequenceArrayOffset(0, sequences.length - 1), 0);
		this.sequences = sequences;
	}
	
	public boolean offer(final E e) {
		if (null == e) {
			throw new NullPointerException("element cannot be null");
		}
		
		final long mask = this.capacity - 1;
		final long[] sequences = this.sequences;
		final E[] buffer = this.buffer;
		
		while (true) {
			final long currentTail = tail;
			final long sequenceOffset = sequenceArrayOffset(currentTail, mask);
			final long sequence = UnsafeApi.getLongVolatile(sequences, sequenceOffset);
			
			if (sequence < currentTail) {
				return false;
			}
			
			if (UnsafeApi.compareAndSetLong(this, TAIL_OFFSET, currentTail, currentTail + 1L)) {
				UnsafeApi.putReference(buffer, sequenceToBufferOffset(currentTail, mask), e);
				UnsafeApi.putLongRelease(sequences, sequenceOffset, currentTail + 1L);
				return true;
			}
			
			Thread.onSpinWait();
		}
	}
	
	@SuppressWarnings("unchecked")
	public E poll() {
		final long[] sequences = this.sequences;
		final E[] buffer = this.buffer;
		final long mask = this.capacity - 1;
		
		while (true) {
			final long currentHead = head;
			final long sequenceOffset = sequenceArrayOffset(currentHead, mask);
			final long sequence = UnsafeApi.getLongVolatile(sequences, sequenceOffset);
			final long attemptedHead = currentHead + 1;
			
			if (sequence < attemptedHead) {
				return null;
			}
			
			if (UnsafeApi.compareAndSetLong(this, HEAD_OFFSET, currentHead, attemptedHead)) {
				final long elementOffset = sequenceToBufferOffset(currentHead, mask);
				final Object e = UnsafeApi.getReference(buffer, elementOffset);
				UnsafeApi.putReference(buffer, elementOffset, null);
				UnsafeApi.putLongRelease(sequences, sequenceOffset, attemptedHead + mask); 
			
				return (E)e;
			}
			
			Thread.onSpinWait();
		}
	}
	
	@SuppressWarnings("unchecked")
	public E peek() {
		final long[] sequences = this.sequences;
		final E[] buffer = this.buffer;
		final long mask = this.capacity - 1;
		
		while (true) {
			final long currentHead = head;
			final long sequenceOffset = sequenceArrayOffset(currentHead, mask);
			final long sequence = UnsafeApi.getLongVolatile(sequences, sequenceOffset);
			final long attemptedHead = currentHead + 1L;
			
			if (sequence < attemptedHead) {
				return null;
			}
			
			if (sequence == attemptedHead) {
				final long elementOffset = sequenceToBufferOffset(currentHead, mask);
				final Object e = UnsafeApi.getReference(buffer, elementOffset);
				
				if (currentHead == head) {
					return (E)e;
				}
			}
			
			Thread.onSpinWait();
		}
	}
	
	public int drain(final Consumer<E> elementConsumer) {
		return drain(elementConsumer, size());
	}
	
	public int drain(final Consumer<E> elementConsumer, final int limit) {
		int count = 0;
		E e;
		
		while (count < limit && null != (e = poll())) {
			elementConsumer.accept(e);
			++count;
		}
		
		return count;
	}
	
	public int drainTo(final Collection<? super E> target, final int limit) {
		int count = 0;
		
		while(count < limit) {
			final E e = poll();
			if (null == e) {
				break;
			}
			
			target.add(e);
			++count;
		}
		
		return count;
	}
	
	private static long sequenceArrayOffset(final long sequence, final long mask) {
		return SEQUENCES_ARRAY_BASE + ((sequence & mask) << 3);
	}
}
