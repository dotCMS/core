package com.dotcms.cache.lettuce;

import com.dotmarketing.util.Config;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.control.Try;

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
     */
    void set (final K key, final V value);

    /**
     * Set a value with ttl in millis
     * @param key K
     * @param value V
     * @param ttlMillis long
     */
    void set (final K key, final V value, final long ttlMillis);

    /**
     * Set the value of a key, only if the key does not exist.
     * @param key  K
     * @param value V
     * @return V
     */
    V setIfAbsent (final K key, final V value);

    /**
     * Set the value only if the key is present
     * @param key
     * @param value
     * @return V returns null of the operation is not successfull, otherwise returns value
     */
    V setIfPresent (final K key, final V value);

    /**
     * Get a value
     * @param key K
     * @return
     */
    V get(final K key);

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
}

