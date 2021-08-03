package com.dotcms.cache.lettuce;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.CompressionCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DirContextDnsResolver;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.vavr.control.Try;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MasterReplicaLettuceClient<K, V> implements RedisClient<K, V> {

    private final static String OK_RESPONSE = "OK";
    private final List<RedisURI> redisUris = Arrays
                    .asList(Config.getStringArrayProperty("REDIS_LETTUCECLIENT_URLS",
                                    new String[] {"redis://password@oboxturbo"}))
                    .stream()
                    .map(u -> RedisURI.create(u))
                    .collect(Collectors.toList());

    private final int timeout = Config.getIntProperty("REDIS_LETTUCECLIENT_TIMEOUT_MS", 3000);
    private final int minIdleConnections = Config.getIntProperty("REDIS_LETTUCECLIENT_MIN_IDLE_CONNECTIONS", 2);
    private final int maxConnections = Config.getIntProperty("REDIS_LETTUCECLIENT_MAX_CONNECTIONS", 5);
    private final GenericObjectPool<StatefulRedisConnection<K, V>> pool;
    public MasterReplicaLettuceClient() {

        this.pool = buildPool();
    }

    public StatefulRedisConnection<K, V> getConn() {
        return Try.of(() -> pool.borrowObject()).onFailure(
                        e -> Logger.warnAndDebug(MasterReplicaLettuceClient.class, "redis unable to connect: " + e, e))
                        .getOrNull();

    }

    @Override
    public boolean isOpen (final StatefulConnection<K, V> connection) {

        return null != connection && connection.isOpen();
    }

    @Override
    public boolean ping () {

        boolean validPing = false;
        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                conn.sync().ping();
                validPing = true;
            }
        } catch (Exception e) {

            Logger.debug(this, e.getMessage());
        }

        return validPing;
    }

    @Override
    public V echo (final V msg) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().echo(msg);
            }
        }

        return null;
    }

    @Override
    public void set (final K key, final V value) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                conn.sync().set(key, value);
            }
        }
    }

    @Override
    public void set (final K key, final V value, final long ttlMillis) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                conn.sync().set(key, value,
                        SetArgs.Builder.px(ttlMillis));
            }
        }
    }

    @Override
    public V setIfAbsent (final K key, final V value) {

        V valueToReturn = value;

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                if (!conn.sync().setnx(key, value)) {

                    valueToReturn = this.get(key);
                }
            }
        }

        return valueToReturn;
    }

    @Override
    public V setIfPresent (final K key, final V value) {

        V valueToReturn = null;

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                if(OK_RESPONSE.equalsIgnoreCase(conn.sync().set(key, value,
                        SetArgs.Builder.xx()))) {

                    valueToReturn = value;
                }

            }
        }

        return valueToReturn;
    }

    @Override
    public long ttlMillis (final K key) {

        long ttlMillis = -2;

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                ttlMillis = conn.sync().pttl(key);
            }
        }

        return ttlMillis;
    }

    @Override
    public V get(final K key) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().get(key);
            }
        }

        return null;
    }

    @Override
    public V delete(final K key) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {
                return conn.sync().getdel(key);
            }
        }

        return null;
    }

    private GenericObjectPool<StatefulRedisConnection<K, V>> buildPool() {

        final GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(this.minIdleConnections);
        config.setMaxTotal(this.maxConnections);

        final DefaultClientResources clientResources = // Does not cache DNS lookups
                        DefaultClientResources.builder().dnsResolver(new DirContextDnsResolver()).build();

        final io.lettuce.core.RedisClient lettuceClient = io.lettuce.core.RedisClient.create(clientResources);

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
