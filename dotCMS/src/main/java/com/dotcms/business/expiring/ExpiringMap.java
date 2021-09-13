package com.dotcms.business.expiring;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * An expiring map is basically a simple abstraction of a {@link java.util.Map} with some capabilities to
 * add entries with an expire ttl
 * @author jsanca
 */
public interface ExpiringMap<K, V> {

    /**
     * Expiring Map can use an strategy when the put method is being called without any timeout information.
     * By default the implementation spend 3 seconds to remove the entry from the cache.
     * @return ExpiringEntryStrategy
     */
    ExpiringEntryStrategy<K,V> getExpiringEntryStrategy();

    /**
     * Put a value with a key, it will live on the map for the millisecond on millisTtl parameter
     * @param key     K
     * @param value   V
     * @param millisTtl {@link Long}
     * @return V
     */
    default V put(final K key, final V value, final long millisTtl) {
        return this.put(key,value, millisTtl, TimeUnit.MILLISECONDS);
    }

    /**
     * Put a value with a key, it will live on the map for the duration time
     * @param key       K
     * @param value     V
     * @param duration {@link Duration}
     * @return V
     */
    default V put(final K key, final V value, final Duration duration) {
        return this.put(key, value, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Put a value with a key, it will live on the map for the duration time specified on ttl with the time unit set on unit parameter
     * @param key      K
     * @param value    V
     * @param ttl      {@link Long} timeout in {@link TimeUnit}
     * @param unit    {@link TimeUnit} unit for the ttl parameter
     * @return V
     */
    V put(K key, V value, final long ttl, final TimeUnit unit);

    /**
     * This allows expiration based on the default ttt used when built the caffeine cache.
     * @param key K
     * @param value V
     * @param useCacheTtl {@link boolean} useCacheTtl if true, the entry will expire using the ttl specified when the the internal caffeine cache got built not the {@link ExpiringEntryStrategy}.
     * @return
     */
    V put(final K key, final V value, final boolean useCacheTtl);

    /**
     *  Put a value with a key, will use the {@link ExpiringEntryStrategy} in other to figure out the timeout for the entry
     * @param key    K
     * @param value  V
     * @return V
     */
    V put(K key, V value);

    /**
     * Returns true if contains the key
     * @param key K
     * @return Boolean
     */
    boolean containsKey(K key);

    /**
     * Removes the entry associated to the key from the map
     * @param key K
     * @return V
     */
    V remove(K key);

    /**
     * Get the values asociated  to the key, null if not any.
     * @param key K
     * @return VS
     */
    V get(K key);

} // E:O:F:ExpiringMap.
