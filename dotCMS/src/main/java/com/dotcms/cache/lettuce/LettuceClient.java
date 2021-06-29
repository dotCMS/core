package com.dotcms.cache.lettuce;

import io.lettuce.core.api.StatefulRedisConnection;

public interface LettuceClient<K, V> {



    public StatefulRedisConnection<K, V> get();
}
