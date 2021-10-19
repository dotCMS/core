package com.dotcms.cache.lettuce;

import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Redis client
 * This client does:
 * - ping
 * - echo
 * - set
 * - get
 * - delete
 * - flush
 * - scan keys
 * - handle hashes
 * - atomic integer
 * - pub and sub.
 *
 * Some operation may have ttl and also a sync/async versions.
 *
 * @param <K>
 * @param <V>
 */
public interface RedisClient<K, V> {

    /**
     * Get a redis connection
     * @return Object
     */
    //StatefulRedisConnection<K, V> getConn();
    Object getConnection();

    /**
     * True if it is a valid open connection
     * @param connection {@link StatefulConnection}
     * @return boolean
     */
    boolean isOpen (final StatefulConnection connection);

    /**
     * Ping to the server
     * @return boolean
     */
    boolean ping ();

    /**
     * Do Echo to the server
     * @param msg V
     * @return V
     */
    V echo (final V msg);

    /**
     * Set a value
     * @param key K
     * @param value V
     * @return boolean true is set
     */
    SetResult set (final K key, final V value);

    /**
     * Set a value with ttl in millis
     * @param key K
     * @param value V
     * @param ttlMillis long
     */
    void set (final K key, final V value, final long ttlMillis);

    /**
     * Set a value async
     * @param key K
     * @param value V
     * @return Future String
     */
    Future<String> setAsync (final K key, final V value);

    /**
     * Set a value async with ttl in millis
     * if ttl is -1, wil persist the key without ttl
     * @param key K
     * @param value V
     * @param ttlMillis long
     * @return Future String
     */
    Future<String> setAsync (final K key, final V value, final long ttlMillis);

    /**
     * Add members
     * @param key K
     * @param values V
     * @return long
     */
    long addMembers (final K key, final V... values);

    /**
     * Add members async
     * @param key K
     * @param values V
     * @return Future long
     */
    Future<Long> addAsyncMembers (final K key, final V... values);

    /**
     * Set the value of a key, only if the key does not exist.
     * @param key  K
     * @param value V
     * @return SetResult
     */
    SetResult setIfAbsent (final K key, final V value);

    /**
     * Set the value only if the key is present
     * @param key
     * @param value
     * @return V returns null of the operation is not successful, otherwise returns value
     */
    V setIfPresent (final K key, final V value);

    /**
     * Get a value
     * @param key K
     * @return
     */
    V get(final K key);

    /**
     * Get members
     * @param key
     * @return Set
     */
    Set<V> getMembers (final K key);
    /**
     * Return ttl in millis for a key
     * @param key K
     * @return long
     */
    long ttlMillis (final K key);

    /**
     * Delete the key and returns the value associated (null if does not exists)
     * @param key K
     * @return V
     */
    V delete(final K key);

    /**
     * Deletes one of more keys, returns the number of keys already deleted
     * @param keys Array of K
     * @return Long number of keys deletes
     */
    long delete(final K... keys);

    /**
     * Non Blocking Deletes one of more keys, returns the number of keys already deleted
     * @param keys Array of K
     * @return Future Long number of keys deletes
     */
    Future<Long> deleteNonBlocking(final K... keys);

    /**
     * Scan each the key (one by one, the results are consumed by keyConsumer
     * @param matchesPattern {@link String} matches pattern
     * @param keyBatchingSize {@link Integer} how many records do you want to fetch by iteration
     * @param keyConsumer {@link Consumer} consumer for each key
     */
    void scanEachKey(final String matchesPattern, int keyBatchingSize, final Consumer<K> keyConsumer);

    /**
     * Scan the keys, the results are consumed by keyConsumer that receives a collection of keys
     * @param matchesPattern {@link String} matches pattern
     * @param keyBatchingSize {@link Integer} how many records do you want to fetch by iteration
     * @param keyConsumer {@link Consumer} consumer a collection each key
     */
    void scanKeys(final String matchesPattern, int keyBatchingSize, final Consumer<Collection<K>> keyConsumer);

    /// TYPES
    ///// HASHES

    /**
     * Returns true if exists the hash and field
     * @param key    K
     * @param field  K
     * @return boolean
     */
    boolean existsHash (K key, K field);

    /**
     * Get the value of the key, field
     * @param key   K
     * @param field K
     * @return V
     */
    V getHash(K key, K field);

    /**
     * Get all fields
     * @param key K
     * @return Map, field -> value
     */
    Map<K, V> getHash(K key);

    /**
     * Get the fields associated to the key
     * @param key K
     * @return Set of K
     */
    Set<K> fieldsHash (K key);

    /**
     * Get a list of the field value associated to the key
     * @param key     K
     * @param fields  Array of K
     * @return List, K -> V
     */
    List<Map.Entry<K, V>> getHash(K key, K... fields);

    /**
     * Set a hash, field -> value
     * @param key K
     * @param map Map, field -> value
     * @return SetResult
     */
    SetResult setHash(K key, Map<K, V> map);

    /**
     * Set hash entry
     * @param key K
     * @param field K
     * @param value V
     * @return SetResult
     */
    SetResult setHash(K key, K field, V value);

    /**
     * Delete the key and returns the value associated (null if does not exists)
     * @param key K
     * @param fields Array of K
     * @return long number of fields deleted (-1 if can not delete)
     */
    long deleteHash(final K key, K... fields);

    ///// INCR

    /**
     * Increment a key by one
     * @param key K
     * @return long current counter (if fail -1)
     */
    long incrementOne(K key);

    /**
     * Increment a key by "amount" parameter
     * @param key    L
     * @param amount long
     * @return long current counter (if fail -1)
     */
    long increment(K key, long amount);

    /**
     * Async Increment a key by one
     * @param key K
     * @return Future long current counter (if fail -1)
     */
    Future<Long> incrementOneAsync (final K key);

    /**
     * Async Increment a key by "amount" parameter
     * @param key    L
     * @param amount long
     * @return Future long current counter (if fail -1)
     */
    Future<Long> incrementAsync (final K key, final long amount);

    /**
     * get the current value of increment
     * @param key K
     * @return Long -1 if does not exists.
     */
    long getIncrement (final K key);

    ////// Streams Pub/Sub

    /**
     * Subscribe a msg consumer to a channel
     * @param messageConsumer  {@link Consumer}
     * @param channel          {@link String}
     * @return String the subscriber id
     */
    default String subscribe (final Consumer<V> messageConsumer, final K channel) {

        return null;
    }

    /**
     * Subscribe a msg consumer to a channel
     * @param messageConsumer  {@link Consumer}
     * @param channel          {@link String}
     * @param subscriberInstanceId {@link String} optional subscriber instance id (in case you do not want an autogenerated)
     * @return String the subscriber id
     */
    default void subscribe (final Consumer<V> messageConsumer, final K channel, final String subscriberInstanceId) {

    }

    /**
     * Unsubscribe a single consumer from a channel
     * @param subscriberId      {@link String}
     * @param channels          {@link String}
     */
    default boolean unsubscribeSubscriber(final String subscriberId, final K channels) { return false; }

    /**
     * UnSubscribe all consumers associated to a channel
     * @param channels          {@link String}
     */
    default boolean unsubscribeSubscriber(final K channels) { return false; }

    /**
     * Return a collection of subscribers to a channel
     * @param channel K
     * @return Collection
     */
    default Collection<Object> getSubscribers (final K channel) {
        return Collections.emptyList();
    }
    /**
     * Publish a message to the channel
     * @param message {@link String}
     * @param channel {@link String}
     */
    default Future<Long> publishMessage (final V message, final K channel) { return ConcurrentUtils.constantFuture(-1L); }

    /**
     * Get the list of channels
     * @return Collection
     */
    default Collection<K> getChannels() {
        return Collections.emptyList();
    }
}

