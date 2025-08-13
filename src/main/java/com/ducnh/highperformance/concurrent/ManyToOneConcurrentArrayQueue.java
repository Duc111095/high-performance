package com.ducnh.highperformance.concurrent;

import java.util.Collection;
import java.util.function.Consumer;

import com.ducnh.highperformance.UnsafeApi;

@SuppressWarnings("removal")
public class ManyToOneConcurrentArrayQueue<E> extends AbstractConcurrentArrayQueue<E>{
	
	public ManyToOneConcurrentArrayQueue(final int requestedCapacity) {
		super(requestedCapacity);
	}
	
	public boolean offer(final E e) {
		if (null == e) {
			throw new NullPointerException("element cannot be null");
		}
		
		final int capacity = this.capacity;
		long currentHead = sharedHeadCache;
		long bufferLimit = currentHead + capacity;
		long currentTail;
		
		do {
			currentTail = tail;
			if (currentTail >= bufferLimit) {
				currentHead = head;
				bufferLimit = currentHead + capacity;
				if (currentTail >= bufferLimit) {
					return false;
				}
				
				UnsafeApi.putLongRelease(this, SHARED_HEAD_CACHE_OFFSET, currentHead);
			}
		}
		while (!UnsafeApi.compareAndSetLong(this, TAIL_OFFSET, currentTail, currentTail + 1));
	
		UnsafeApi.putReferenceRelease(buffer, sequenceToBufferOffset(currentTail, capacity - 1), e);
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public E poll() {
		final long currentHead = head;
		final long elementOffset = sequenceToBufferOffset(currentHead, capacity - 1);
		final Object[] buffer = this.buffer;
		final Object e = UnsafeApi.getReferenceVolatile(buffer, elementOffset);
		
		if (null != e) {
			UnsafeApi.putReference(buffer, elementOffset, null);
			UnsafeApi.putLongRelease(this, HEAD_OFFSET, currentHead + 1);
		}
		
		return (E)e;
 	}
	
	public int drain(final Consumer<E> elementConsumer) {
		return drain(elementConsumer, (int)(tail - head));
	}
	
	@SuppressWarnings("unchecked")
	public int drain(final Consumer<E> elementConsumer, final int limit) {
		final Object[] buffer = this.buffer;
		final long mask = this.capacity - 1;
		final long currentHead = head;
		long nextSequence = currentHead;
		final long limitSequence = nextSequence + limit;
		
		while (nextSequence < limitSequence) {
			final long elementOffset = sequenceToBufferOffset(nextSequence, mask);
			final Object item = UnsafeApi.getReferenceVolatile(buffer, elementOffset);
		
			if (null == item) {
				break;
			}
			
			UnsafeApi.putReferenceRelease(buffer, elementOffset, null);
			nextSequence++;
			UnsafeApi.putLongRelease(this, HEAD_OFFSET, nextSequence);
			elementConsumer.accept((E)item);
		}
		
		return (int) (nextSequence - currentHead);
	}
	
	@SuppressWarnings("unchecked")
	public int drainTo(final Collection<? super E> target, final int limit) {
		final Object[] buffer = this.buffer;
		final long mask = this.capacity - 1;
		long nextSequence = head;
		int count = 0;
		
		while (count < limit) {
			final long elementOffset = sequenceToBufferOffset(nextSequence, mask);
			final Object e = UnsafeApi.getReferenceVolatile(buffer, elementOffset);
			if (null == e) {
				break;
			}
			
			UnsafeApi.putReferenceRelease(buffer, elementOffset, null);
			nextSequence++;
			UnsafeApi.putLongRelease(this, HEAD_OFFSET, nextSequence);
			count++;
			target.add((E)e);
		}
		
		return count;
	}
}
