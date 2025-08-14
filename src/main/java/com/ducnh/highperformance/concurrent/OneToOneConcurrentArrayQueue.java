package com.ducnh.highperformance.concurrent;

import java.util.Collection;
import java.util.function.Consumer;

import com.ducnh.highperformance.UnsafeApi;

@SuppressWarnings("removal")
public class OneToOneConcurrentArrayQueue<E> extends AbstractConcurrentArrayQueue<E>{
	
	public OneToOneConcurrentArrayQueue(final int requestedCapacity) {
		super(requestedCapacity);
	}
	
	public boolean offer(final E e) {
		if (null == e) {
			throw new NullPointerException("Null is not a valid element");
		}
		
		final int capacity = this.capacity;
		long currentHead = headCache;
		long bufferLimit = currentHead + capacity;
		final long currentTail = tail;
		if (currentTail >= bufferLimit) {
			currentHead = head;
			bufferLimit = currentHead + capacity;
			if (currentTail >= bufferLimit) {
				return false;
			}
			
			headCache = currentHead;
		}
		
		final long elementOffset = sequenceToBufferOffset(currentTail, capacity - 1);
		
		UnsafeApi.putReferenceRelease(buffer, elementOffset, e);
		UnsafeApi.putLongRelease(this, TAIL_OFFSET, currentTail + 1);
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public E poll() {
		final Object[] buffer = this.buffer;
		final long currentHead = head;
		final long elementOffset = sequenceToBufferOffset(currentHead, capacity - 1);
		
		final Object e = UnsafeApi.getReferenceVolatile(buffer, elementOffset);
		
		if (null != e) {
			UnsafeApi.putReferenceRelease(buffer, elementOffset, null);
			UnsafeApi.putLongRelease(this, HEAD_OFFSET, currentHead + 1);
		}
		
		return (E)e;
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
		
		return (int)(nextSequence - currentHead);
	}
	
	@SuppressWarnings("unchecked")
	public int drainTo(final Collection<? super E> target, final int limit) {
		final Object[] buffer = this.buffer;
		final long mask = this.capacity - 1;
		long nextSequence = head;
		int count = 0;
		
		while (count < limit) {
			final long elementOffset = sequenceToBufferOffset(nextSequence, mask);
			final Object item = UnsafeApi.getReferenceVolatile(buffer, elementOffset);
			if (null == item) {
				break;
			}
			
			UnsafeApi.putReferenceRelease(buffer, elementOffset, null);
			nextSequence++;
			UnsafeApi.putLongRelease(this, HEAD_OFFSET, nextSequence);
			count++;
			target.add((E)item);
		}
		
		return count;
	}

	@Override
	public int drain(Consumer<E> elementConsumer) {
		return drain(elementConsumer, (int)(tail - head));
	}
}
