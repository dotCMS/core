package com.dotcms.cache.lettuce;

import com.dotcms.dotpubsub.LettucePubSubImpl;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.Function0;

public class DotRedisClient {

    
    private static RedisClient buildClient() {
        
        try {
            String clientUrl = Config.getStringProperty("REDIS_CLIENT_URL", "redis://password@oboxturbo");
            RedisClient client = RedisClient.create(clientUrl);
            return client;
        } catch (Exception e) {
            Logger.warnAndDebug(LettucePubSubImpl.class, e);
            throw new DotRuntimeException(e);
        }
    }

    private static Function0<RedisClient> client = Function0.of(DotRedisClient::buildClient).memoized();


    public static RedisClient get() {
        return client.apply();
    }
    
    private static StatefulRedisConnection connection;
    public static  StatefulRedisConnection<String, String> getConnection() {
        if (connection == null) {

            RedisClient redisClient = DotRedisClient.get();
            connection = redisClient.connect();
            return connection;
        }
        if (!connection.isOpen()) {
            connection.closeAsync();
            RedisClient redisClient = DotRedisClient.get();
            connection = redisClient.connect();
            return connection;
        }
        return connection;
    }
    
}

