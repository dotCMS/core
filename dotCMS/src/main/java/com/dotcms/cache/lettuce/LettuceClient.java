package com.dotcms.cache.lettuce;

import com.dotmarketing.util.Config;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.Function0;
import io.vavr.control.Try;

public interface LettuceClient {

    static LettuceClient buildClient() {
        Class clazz = Try.of(() -> Class.forName(Config.getStringProperty("redis.lettucecache.connection",
                        "com.dotcms.cache.lettuce.MasterReplicaLettuceClient"))).get();
        return Try.of(() -> (LettuceClient) clazz.newInstance()).getOrElse(MasterReplicaLettuceClient.INSTANCE);

    }

    static Function0<LettuceClient> client = Function0.of(LettuceClient::buildClient).memoized();


    public static LettuceClient getInstance() {

        return client.apply();

    }

    public StatefulRedisConnection<String, Object> get();
}

