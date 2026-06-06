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

    /**
     * Returns the named client, creating it with the given codec on first use.
     * <p><b>Note:</b> the {@code codec} is only applied when this is the first call for {@code name} AND no
     * {@code LETTUCE_CLIENT_CLASS} override is configured. Clients are cached per name, so a later call with a
     * different codec for an already-created name returns the existing client and the codec is ignored.
     *
     * @param name  the client name (cache key)
     * @param codec the codec to use when the client is created for the first time
     * @return the (possibly already-cached) client for {@code name}
     */
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

                    // NOTE: when LETTUCE_CLIENT_CLASS is set, the client is built via its no-arg constructor and
                    // the defaultClientSupplier (including any custom codec) is NOT used. Clients are also cached
                    // per name, so the first getClient(name, ...) call wins for that name regardless of codec.
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
