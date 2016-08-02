package org.topbraid.mauiserver.persistence;

/**
 * A trivial non-persistent implementation of {@link ObjectStore}
 * that simply keeps the value in a variable, for testing.
 */
public class SimpleStore<T> implements ObjectStore<T> {
	private T contents;
	
	public SimpleStore() {
		this(null);
	}
	
	public SimpleStore(T initialContents) {
		contents = initialContents;
	}
	
	@Override
	public boolean contains() {
		return contents == null;
	}

	@Override
	public T get() {
		return contents;
	}

	@Override
	public void put(T storable) {
		contents = storable;
	}

	@Override
	public void delete() {
		contents = null;
	}
}
