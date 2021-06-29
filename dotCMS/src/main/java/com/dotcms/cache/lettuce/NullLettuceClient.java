package com.dotcms.cache.lettuce;

import io.lettuce.core.api.StatefulRedisConnection;

public class NullLettuceClient<K, V> implements LettuceClient {

    @Override
    public StatefulRedisConnection<K, V> get() {
        return null;
    }

}
