package com.dotcms.cache.lettuce;

import io.lettuce.core.api.StatefulRedisConnection;

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
