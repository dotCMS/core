package com.dotcms.cache.lettuce;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import io.lettuce.core.codec.RedisCodec;
import io.vavr.control.Try;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory to provide clients for redis.
 * @author jsanca
 */
public class RedisClientFactory {

    private final static Map<String, RedisClient<String, Object>> clientMap = new ConcurrentHashMap<>();

    public static RedisClient<String, Object> getClient(final String name) {

        RedisClient<String, Object> redisClient = clientMap.get(name);
        if (null == redisClient) {

            synchronized (RedisClientFactory.class) {

                redisClient = clientMap.get(name);

                if (null == redisClient) {

                    final String clazz = Config.getStringProperty("LETTUCE_CLIENT_CLASS",
                            MasterReplicaLettuceClient.class.getCanonicalName());

                    redisClient = Try.of(() -> (RedisClient) RedisClient.class.forName(clazz).newInstance())
                            .getOrElse(new MasterReplicaLettuceClient<>());
                    clientMap.put(name, redisClient);
                }
            }
        }

        return redisClient;
    }

    public static RedisClient<String, Object> getClient(final String name, final RedisCodec<String, Object> codec) {

        RedisClient<String, Object> redisClient = clientMap.get(name);
        if (null == redisClient) {

            synchronized (RedisClientFactory.class) {

                redisClient = clientMap.get(name);

                if (null == redisClient) {

                    final String clazz = Config.getStringProperty("LETTUCE_CLIENT_CLASS",
                            MasterReplicaLettuceClient.class.getCanonicalName());

                    redisClient = Try.of(() -> (RedisClient) RedisClient.class.forName(clazz).newInstance())
                            .getOrElse(new MasterReplicaLettuceClient<>(codec,
                                    APILocator.getShortyAPI().shortify(ClusterFactory.getClusterId())));
                    clientMap.put(name, redisClient);
                }
            }
        }

        return redisClient;
    }
}
