package com.dotcms.cache.lettuce;

import com.dotmarketing.util.Config;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.control.Try;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Redis client
 * @param <K>
 * @param <V>
 */
public interface RedisClient<K, V> {

    default RedisClient<K, V> getClient() {
        String clazz = Config.getStringProperty("LETTUCE_CLIENT_CLASS",
                        MasterReplicaLettuceClient.class.getCanonicalName());

        return Try.of(() -> (RedisClient) RedisClient.class.forName(clazz).newInstance())
                        .getOrElse(new MasterReplicaLettuceClient<>());
    }

    /**
     * Get a redis connection
     * @return StatefulRedisConnection
     */
    StatefulRedisConnection<K, V> getConn();

    /**
     * True if it is a valid open connection
     * @param connection {@link StatefulConnection}
     * @return boolean
     */
    boolean isOpen (final StatefulConnection<K, V> connection);

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
     * Flush all cache
     * @return
     */
    String flushAll();

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

}

