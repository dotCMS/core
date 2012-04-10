package com.eng.achecker.parsing;

import java.util.Iterator;

public class EmptyIterable<T> implements Iterable<T> {
	
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			public boolean hasNext() {
				return false;
			}

			public T next() {
				throw new IllegalStateException("Not supported method");
			}

			public void remove() {
				throw new IllegalStateException("Not supported method");
			}
		};
	}
}
