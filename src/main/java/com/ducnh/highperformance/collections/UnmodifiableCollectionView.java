package com.ducnh.highperformance.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

public class UnmodifiableCollectionView<V, E> extends AbstractCollection<V>{
	private final ReusableIterator iterator = new ReusableIterator();
	private final Function<E, V> viewer;
	private final Collection<E> elements;

	public UnmodifiableCollectionView(final Function<E, V> viewer, Collection<E> elements) {
		this.viewer = viewer;
		this.elements = elements;
	}
	
	public int size() {
		return elements.size();
	}
	
	public ReusableIterator iterator() {
		return iterator.reset();
	}
	
	public final class ReusableIterator implements Iterator<V> {
		private Iterator<E> delegate;
		
		public ReusableIterator() {}
		
		public boolean hasNext() {
			return delegate.hasNext();
		}
		
		public V next() {
			return viewer.apply(delegate.next());
		}
		
		private ReusableIterator reset() {
			delegate = elements.iterator();
			return this;
		}
	}
}
