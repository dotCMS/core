package com.dotcms.cache.lettuce;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.CompressionCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DirContextDnsResolver;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.vavr.control.Try;

public class MasterReplicaLettuceClient<K, V> implements LettuceClient<K, V> {

    private final List<RedisURI> redisUris = Arrays
                    .asList(Config.getStringArrayProperty("REDIS_LETTUCECLIENT_URLS",
                                    new String[] {"redis://password@oboxturbo"}))
                    .stream()
                    .map(u -> RedisURI.create(u))
                    .collect(Collectors.toList());

    private final int timeout = Config.getIntProperty("REDIS_LETTUCECLIENT_TIMEOUT_MS", 3000);
    private final int maxConnections = Config.getIntProperty("REDIS_LETTUCECLIENT_MAX_CONNECTIONS", 5);
    private final GenericObjectPool<StatefulRedisConnection<K, V>> pool;

    public MasterReplicaLettuceClient() {
        pool = buildPool();

    }

    public StatefulRedisConnection<K, V> get() {
        return Try.of(() -> pool.borrowObject()).onFailure(
                        e -> Logger.warnAndDebug(MasterReplicaLettuceClient.class, "redis unable to connect: " + e, e))
                        .getOrNull();

    }

    private GenericObjectPool<StatefulRedisConnection<K, V>> buildPool() {

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(2);
        config.setMaxTotal(maxConnections);

        final DefaultClientResources clientResources =
                        DefaultClientResources.builder().dnsResolver(new DirContextDnsResolver()) // Does not cache DNS
                                                                                                  // lookups
                                        .build();

        final RedisClient lettuceClient = RedisClient.create(clientResources);

        return ConnectionPoolSupport.createGenericObjectPool(() -> {

            try {
                StatefulRedisConnection<K, V> connection =
                                (StatefulRedisConnection<K, V>) MasterReplica
                                                .connect(lettuceClient,
                                                                CompressionCodec.valueCompressor(new DotObjectCodec(),
                                                                                CompressionCodec.CompressionType.GZIP),
                                                                redisUris);

                ((StatefulRedisMasterReplicaConnection) connection).setReadFrom(ReadFrom.REPLICA_PREFERRED);
                if (timeout > 0) {
                    connection.setTimeout(Duration.ofMillis(timeout));
                }
                return connection;
            } catch (Exception e) {
                Logger.warnAndDebug(this.getClass(), e);
                throw new DotStateException(e);
            }
        }, config, true);

    }

}
