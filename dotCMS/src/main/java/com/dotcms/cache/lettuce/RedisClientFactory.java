package com.dotcms.cache.lettuce;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.lettuce.core.codec.RedisCodec;
import io.vavr.control.Try;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Factory to provide clients for redis.
 * @author jsanca
 */
public class RedisClientFactory {

    private final static Map<String, RedisClient<String, Object>> clientMap = new ConcurrentHashMap<>();

    public static RedisClient<String, Object> getClient(final String name) {

        return getOrCreate(name, MasterReplicaLettuceClient::new);
    }

    public static RedisClient<String, Object> getClient(final String name, final RedisCodec<String, Object> codec) {

        return getOrCreate(name, () -> new MasterReplicaLettuceClient<>(codec,
                APILocator.getShortyAPI().shortify(ClusterFactory.getClusterId())));
    }

    private static RedisClient<String, Object> getOrCreate(final String name,
            final Supplier<RedisClient<String, Object>> defaultClientSupplier) {

        RedisClient<String, Object> redisClient = clientMap.get(name);
        if (null == redisClient) {

            synchronized (RedisClientFactory.class) {

                redisClient = clientMap.get(name);

                if (null == redisClient) {

                    final String clazz = Config.getStringProperty("LETTUCE_CLIENT_CLASS",
                            MasterReplicaLettuceClient.class.getCanonicalName());

                    redisClient = Try.of(() ->
                                    (RedisClient<String, Object>) Class.forName(clazz)
                                            .getDeclaredConstructor().newInstance())
                            .onFailure(e -> Logger.warnAndDebug(RedisClientFactory.class,
                                    "Unable to instantiate LETTUCE_CLIENT_CLASS '" + clazz
                                            + "', falling back to the default client: " + e.getMessage(), e))
                            .getOrElseGet(e -> defaultClientSupplier.get());
                    clientMap.put(name, redisClient);
                }
            }
        }

        return redisClient;
    }
}
