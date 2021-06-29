package com.dotcms.cache.lettuce;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Function0;
import io.vavr.control.Try;

public interface LettuceClient {

    
    static LettuceClient buildClient() {
        Class clazz = Try.of(() -> Class.forName(Config.getStringProperty("redis.lettucecache.lettuceclient",
                        "com.dotcms.cache.lettuce.MasterReplicaLettuceClient"))).get();
        return Try.of(() -> (LettuceClient) clazz.newInstance()).getOrElse(MasterReplicaLettuceClient.INSTANCE);
    }

    static Function0<LettuceClient> client = Function0.of(LettuceClient::buildClient).memoized();

    
    
    static final LettuceClient nullClient = new NullLettuceClient();

    public static LettuceClient getInstance() {

        try {
            return client.apply();
        }
        catch(Exception e) {
            Logger.warn(LettuceClient.class, "Unable to connect to Redis:" + e);
        }
        return nullClient;

    }

    public LettuceConnectionWrapper get();
}

