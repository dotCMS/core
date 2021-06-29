package com.dotcms.cache.lettuce;

import com.dotmarketing.util.Config;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.control.Try;

public interface LettuceClient<K, V> {

    default LettuceClient<K, V> getClient() {
        String clazz = Config.getStringProperty("LETTUCE_CLIENT_CLASS",
                        MasterReplicaLettuceClient.class.getCanonicalName());

        return Try.of(() -> (LettuceClient) LettuceClient.class.forName(clazz).newInstance())
                        .getOrElse(new MasterReplicaLettuceClient<>());

    }

    public StatefulRedisConnection<K, V> get();
}
