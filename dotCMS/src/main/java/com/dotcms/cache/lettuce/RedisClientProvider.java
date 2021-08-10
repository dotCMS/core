package com.dotcms.cache.lettuce;

import com.dotmarketing.util.Config;
import io.vavr.control.Try;

public class RedisClientProvider {

    private static RedisClient<String, Object> INSTANCE;

    public static RedisClient<String, Object> getInstance() {

        if (INSTANCE == null) {
            synchronized (RedisClientProvider.class) {
                if (INSTANCE == null) {

                    final String clazz = Config.getStringProperty("LETTUCE_CLIENT_CLASS",
                            MasterReplicaLettuceClient.class.getCanonicalName());

                    INSTANCE = Try.of(() -> (RedisClient) RedisClient.class.forName(clazz).newInstance())
                            .getOrElse(new MasterReplicaLettuceClient<>());
                }
            }
        }

        return INSTANCE;
    }
}
