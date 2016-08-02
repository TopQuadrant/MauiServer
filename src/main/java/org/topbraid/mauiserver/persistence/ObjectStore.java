package org.topbraid.mauiserver.persistence;

/**
 * Storage, possibly persistent, for a single object. Putting null is
 * equivalent to deleting. 
 */
public interface ObjectStore<T> {

	boolean contains();

	T get();

	void put(T storable);

	void delete();
}