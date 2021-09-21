package com.dotcms.cache.lettuce;

import io.lettuce.core.api.StatefulRedisConnection;

/**
 * This adapter just converts an object to a connection, see
 * {@link RedisClient#getConnection()}
 * @author jsanca
 */
public class LettuceAdapter {

    /**
     * Adapt the redis client, to a StatefulRedisConnection
     * @param redisClient
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K,V> StatefulRedisConnection<K, V>  getStatefulRedisConnection(final RedisClient<K, V> redisClient) {

        return (StatefulRedisConnection<K, V>)redisClient.getConnection();
    }
}
