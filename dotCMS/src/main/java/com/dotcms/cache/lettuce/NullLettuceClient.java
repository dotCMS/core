package com.dotcms.cache.lettuce;

import io.lettuce.core.api.StatefulRedisConnection;

public class NullLettuceClient<K, V> implements RedisClient {

    @Override
    public StatefulRedisConnection<K, V> getConn() {
        return null;
    }

}
