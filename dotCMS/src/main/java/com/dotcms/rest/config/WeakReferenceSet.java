package com.dotcms.rest.config;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A set that holds weak references to its elements, allowing them to be garbage collected
 * @param <T>
 */
public class WeakReferenceSet<T> {

    private final Set<WeakReferenceWrapper<T>> references = ConcurrentHashMap.newKeySet();
    private final ReferenceQueue<T> referenceQueue = new ReferenceQueue<>();

    /**
     * Wrapper class for weak references to ensure proper equality and hashCode handling.
     * @param <T>
     */
    private static class WeakReferenceWrapper<T> extends WeakReference<T> {
        private final int hashCode;

        public WeakReferenceWrapper(T referent, ReferenceQueue<T> queue) {
            super(referent, queue);
            this.hashCode = referent != null ? referent.hashCode() : 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof WeakReferenceWrapper)) return false;

            WeakReferenceWrapper<?> other = (WeakReferenceWrapper<?>) obj;
            Object thisRef = this.get();
            Object otherRef = other.get();

            return thisRef != null && thisRef.equals(otherRef);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * clean dead references from the reference queue
     */
    private void cleanupDeadReferences() {
        WeakReferenceWrapper<T> deadRef;
        while ((deadRef = (WeakReferenceWrapper<T>) referenceQueue.poll()) != null) {
            references.remove(deadRef);
        }
    }

    /**
     * Adds classes to the set using weak references.
     */
    public synchronized boolean add(T item) {
        if (item == null) return false;

        cleanupDeadReferences();

        WeakReferenceWrapper<T> wrapper = new WeakReferenceWrapper<>(item, referenceQueue);
        return references.add(wrapper);
    }

    /**
     * Removes a class from the set.
     */
    public synchronized boolean remove(T item) {
        if (item == null) return false;

        cleanupDeadReferences();

        WeakReferenceWrapper<T> wrapper = new WeakReferenceWrapper<>(item, referenceQueue);
        return references.remove(wrapper);
    }

    /**
     * Returns classes that are still alive in the set.
     */
    public synchronized Set<T> getAliveClasses() {
        cleanupDeadReferences();

        Set<T> aliveClasses = ConcurrentHashMap.newKeySet();

        Iterator<WeakReferenceWrapper<T>> iterator = references.iterator();
        while (iterator.hasNext()) {
            WeakReferenceWrapper<T> wrapper = iterator.next();
            T item = wrapper.get();

            if (item != null) {
                aliveClasses.add(item);
            } else {
                // Remove dead references from the set
                iterator.remove();
            }
        }

        return aliveClasses;
    }

    /**
     * Returns the number of alive references in the set.
     */
    public synchronized int size() {
        cleanupDeadReferences();
        return references.size();
    }

    /**
     * Clears the set and the reference queue.
     */
    public synchronized void clear() {
        references.clear();
        // Clear the reference queue
        while (referenceQueue.poll() != null) {
            // Only poll to clear the queue
        }
    }
}
