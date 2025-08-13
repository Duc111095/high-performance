package com.ducnh.highperformance.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import com.ducnh.highperformance.UnsafeApi;

@SuppressWarnings({"deprecation", "removal"})
abstract class ManyToOneConcurrentLinkedQueuePadding1 {
	protected static final long HEAD_OFFSET;
	protected static final long TAIL_OFFSET;
	protected static final long NEXT_OFFSET;
	
	static final class Node<E> {
		E value;
		volatile Node<E> next;
		Node(final E value) {
			this.value = value;
		}
		
		void nextOrdered(final Node<E> next) {
			UnsafeApi.putReferenceRelease(this, NEXT_OFFSET, next);
		} 
	}
	
	static {
		try {
			HEAD_OFFSET = UnsafeApi.objectFieldOffset(
					ManyToOneConcurrentLinkedQueueHead.class.getDeclaredField("head"));
			TAIL_OFFSET = UnsafeApi.objectFieldOffset(
					ManyToOneConcurrentLinkedQueueTail.class.getDeclaredField("tail"));
			NEXT_OFFSET =  UnsafeApi.objectFieldOffset(
					Node.class.getDeclaredField("next"));
		} catch (final Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	byte p000, p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
	byte p016, p017, p018, p019, p020, p021, p022, p023, p024, p025, p026, p027, p028, p029, p030, p031;
	byte p032, p033, p034, p035, p036, p037, p038, p039, p040, p041, p042, p043, p044, p045, p046, p047;
	byte p048, p049, p050, p051, p052, p053, p054, p055, p056, p057, p058, p059, p060, p061, p062, p063;
}

abstract class ManyToOneConcurrentLinkedQueueTail<E> extends ManyToOneConcurrentLinkedQueuePadding1 {
	protected volatile ManyToOneConcurrentLinkedQueue.Node<E> tail;
}

abstract class ManyToOneConcurrentLinkedQueuePadding2<E> extends ManyToOneConcurrentLinkedQueueTail<E>
{
    byte p064, p065, p066, p067, p068, p069, p070, p071, p072, p073, p074, p075, p076, p077, p078, p079;
    byte p080, p081, p082, p083, p084, p085, p086, p087, p088, p089, p090, p091, p092, p093, p094, p095;
    byte p096, p097, p098, p099, p100, p101, p102, p103, p104, p105, p106, p107, p108, p109, p110, p111;
    byte p112, p113, p114, p115, p116, p117, p118, p119, p120, p121, p122, p123, p124, p125, p126, p127;
}

abstract class ManyToOneConcurrentLinkedQueueHead<E> extends ManyToOneConcurrentLinkedQueuePadding2<E> {
	protected volatile ManyToOneConcurrentLinkedQueue.Node<E> head;
}

@SuppressWarnings("removal")
public class ManyToOneConcurrentLinkedQueue<E> extends ManyToOneConcurrentLinkedQueueHead<E> implements Queue<E> {
    byte p128, p129, p130, p131, p132, p133, p134, p135, p136, p137, p138, p139, p140, p142, p143, p144;
    byte p145, p146, p147, p148, p149, p150, p151, p152, p153, p154, p155, p156, p157, p158, p159, p160;
    byte p161, p162, p163, p164, p165, p166, p167, p168, p169, p170, p171, p172, p173, p174, p175, p176;
    byte p177, p178, p179, p180, p181, p182, p183, p184, p185, p186, p187, p189, p190, p191, p192, p193;
    
    private final Node<E> empty = new Node<>(null);
    
    @SuppressWarnings("this-escape")
    public ManyToOneConcurrentLinkedQueue() {
    	headOrdered(empty);
    	UnsafeApi.putReferenceRelease(this, TAIL_OFFSET, empty);
    }
    
    public boolean add(final E e) {
    	return offer(e);
    }
    
    public boolean offer(final E e) {
    	if (null == e) {
    		throw new NullPointerException("element cannot be null");
    	}
    	
    	final Node<E> tail = new Node<>(e);
    	final Node<E> previousTail = swapTail(tail);
    	previousTail.nextOrdered(tail);
    	
    	return true;
    }
    
    public E remove() {
    	final E e = poll();
    	if (null == e) {
    		throw new NoSuchElementException("Queue is empty");
    	}
    	
    	return e;
    }
    
    public E poll() {
    	E value = null;
    	final Node<E> head = this.head;
    	Node<E> next = head.next;
    	
    	if (null != next)  {
    		value = next.value;
    		next.value = null;
    		head.nextOrdered(null);
    		
    		if (null == next.next) {
    			final Node<E> tail = this.tail;
    			if (tail == next && casTail(tail, empty)) {
    				next = empty;
    			}
    		}
    		
    		headOrdered(next);
    	}
    	
    	return value;
    }
    
    public E element() {
    	final E e = peek();
    	if (null == e) {
    		throw new NoSuchElementException("Queue is empty");
    	}
    	
    	return e;
    }
    
    public E peek() {
    	final Node<E> next = head.next;
    	return null != next ? next.value : null;
    }
    
    public int size() {
    	Node<E> head = this.head;
    	final Node<E> tail = this.tail;
    	int size = 0;
    	
    	while (tail != head && size < Integer.MAX_VALUE) {
    		final Node<E> next = head.next;
    		if (null == next) {
    			break;
    		}
    		
    		head = next;
    		++size;
    	}
    	
    	return size;
    }
    
    public boolean isEmpty() {
    	return head == tail;
    }
    
    public boolean contains(final Object o) {
    	throw new UnsupportedOperationException();
    }
    
    public Iterator<E> iterator() {
    	throw new UnsupportedOperationException();
    }
    
    public Object[] toArray() {
    	throw new UnsupportedOperationException();
    }
    
    public <T> T[] toArray(final T[] a) {
    	throw new UnsupportedOperationException();
    }
    
    public boolean remove(final Object o) {
    	throw new UnsupportedOperationException();
    }
    
    public boolean containsAll(final Collection<?> c) {
    	throw new UnsupportedOperationException();
    }
    
    public boolean addAll(final Collection<? extends E> c) {
    	throw new UnsupportedOperationException();
    }
    
    public boolean removeAll(final Collection<?> c) {
    	throw new UnsupportedOperationException();
    }
    
    public boolean retainAll(final Collection<?> c) {
    	throw new UnsupportedOperationException();
    }
    
    public void clear() {
    	throw new UnsupportedOperationException();
    }
    
    public String toString() {
    	final StringBuilder sb = new StringBuilder();
    	sb.append('{');
    	
    	Node<E> head = this.head;
    	final Node<E> tail = this.tail;
    	
    	while (head != tail) {
    		final Node<E> next = head.next;
    		if (null == next) {
    			break;
    		}
    		
    		head = next;
    		
    		sb.append(head.value);
    		sb.append(", ");
    	}
    	
    	if (sb.length() > 1) {
    		sb.setLength(sb.length() - 2);
    	}
    	
    	sb.append('}');
    	
    	return sb.toString();
    }
    
    private void headOrdered(final Node<E> head) {
    	UnsafeApi.putReferenceRelease(this, HEAD_OFFSET, head);
    }
    
    @SuppressWarnings("unchecked")
    private Node<E> swapTail(final Node<E> newTail) {
    	return (Node<E>)UnsafeApi.getAndSetReference(this, TAIL_OFFSET, newTail);
    }
    
    private boolean casTail(final Node<E> expectedNode, final Node<E> updateNode) {
    	return UnsafeApi.compareAndSetReference(this, TAIL_OFFSET, expectedNode, updateNode);
    }
}
