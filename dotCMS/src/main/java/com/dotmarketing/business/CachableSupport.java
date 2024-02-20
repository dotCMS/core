package com.dotmarketing.business;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Provides a bit more functionality than the Cachable interface.
 * In addition to the get, put, and remove methods, it also provides a safe getOrUpdate method.
 * @author jsanca
 * @param <K>
 * @param <V>
 */
public interface CachableSupport<K, V> extends Cachable {

    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * Thread-safe method to retrieve the value from the cache, in case it does not exist the update function will populate the cache.
     * @param key
     * @param updateFunction
     * @return
     */
    default V getOrUpdate(final K key, final Supplier<V> updateFunction) {

        V value;
        readWriteLock.readLock().lock();
        try {

            value = get(key);  // Assuming ‘get’ is your method to fetch the value from the cache
            if (value != null) {
                return value;
            }
        } finally {
            readWriteLock.readLock().unlock();
        }
        readWriteLock.writeLock().lock();
        try {
            // Double-check (in case another thread updated the value)
            value = get(key);
            if (value == null) {
                value = updateFunction.get();  // Call the lambda function to update the value
                if (value != null) {
                    put(key, value);  // Assuming ‘put’ is your method to add the value to the cache
                }
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
        return value;
    }

    /**
     * Gets the value from the cache
     * @param key
     * @return
     */
    V get(K key);

    /**
     *
     * @param key
     * @param value
     * @return
     */
    V put(K key, V value);

    /**
     * Remove the value from the cache
     * @param key
     */
    void remove(K key);
}
