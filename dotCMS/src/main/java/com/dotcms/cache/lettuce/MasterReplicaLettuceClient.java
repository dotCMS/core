package com.dotcms.cache.lettuce;

import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.CompressionCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.output.ValueOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DirContextDnsResolver;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.vavr.control.Try;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MasterReplicaLettuceClient<K, V> implements RedisClient<K, V> {

    private final static String OK_RESPONSE = "OK";
    private final static String ERROR_RESPONSE = "ERROR";
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
    private final RedisCodec<K, V> codec  = CompressionCodec.valueCompressor(new DotObjectCodec(), CompressionCodec.CompressionType.GZIP);
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
    public SetResult set (final K key, final V value) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return OK_RESPONSE.equalsIgnoreCase(conn.sync().set(key, value))?
                        SetResult.SUCCESS: SetResult.FAIL;
            } else {
                return SetResult.NO_CONN;
            }
        }
    }

    @Override
    public Future<String> setAsync (final K key, final V value) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.async().set(key, value);
            }
        }

        return ConcurrentUtils.constantFuture(ERROR_RESPONSE);
    }

    @Override
    public long addMembers (final K key, final V... values) {

        long membersCount = 0;

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                membersCount = conn.sync().sadd(key, values);
            }
        }

        return membersCount;
    }

    @Override
    public Future<Long> addAsyncMembers (final K key, final V... values) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.async().sadd(key, values);
            }
        }

        return ConcurrentUtils.constantFuture(0l);
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
    public Future<String> setAsync (final K key, final V value, final long ttlMillis) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return ttlMillis == -1?
                        conn.async().set(key, value):
                        conn.async().set(key, value, SetArgs.Builder.px(ttlMillis));
            }
        }

        return ConcurrentUtils.constantFuture(ERROR_RESPONSE);
    }

    @Override
    public SetResult setIfAbsent (final K key, final V value) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            return  this.isOpen(conn)?
                    (
                            conn.sync().setnx(key, value)?
                                SetResult.SUCCESS: SetResult.FAIL
                    ): SetResult.NO_CONN;
        }
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

                try {
                    return conn.sync().get(key);
                } catch (RedisCommandTimeoutException e) {
                        throw new CacheTimeoutException(e);
                }
            }
        }

        return null;
    }

    @Override
    public Set<V> getMembers (final K key) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().smembers(key);
            }
        }

        return Collections.emptySet();
    }

    @Override
    public void scanEachKey(final String matchesPattern, int keyBatchingSize, final Consumer<K> keyConsumer) {

        KeyScanCursor<K> scanCursor = null;

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (isOpen(conn)) {

                final RedisCommands<K, V> syncCommand = conn.sync();
                final ScanArgs scanArgs = ScanArgs.Builder.matches(matchesPattern).limit(keyBatchingSize);
                do {

                    scanCursor = scanCursor == null?
                            syncCommand.scan(scanArgs):syncCommand.scan(scanCursor, scanArgs);

                    scanCursor.getKeys().forEach(keyConsumer);
                } while (!scanCursor.isFinished());
            }
        }
    }

    public void scanKeys(final String matchesPattern, int keyBatchingSize,
                         final Consumer<Collection<K>> keyConsumer) {

        KeyScanCursor<K> scanCursor = null;

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (isOpen(conn)) {

                final RedisCommands<K, V> syncCommand = conn.sync();
                final ScanArgs scanArgs = ScanArgs.Builder.matches(matchesPattern).limit(keyBatchingSize);
                do {

                    scanCursor = scanCursor == null?
                            syncCommand.scan(scanArgs):syncCommand.scan(scanCursor, scanArgs);

                    keyConsumer.accept(scanCursor.getKeys());
                } while (!scanCursor.isFinished());
            }
        }
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

    @Override
    public long delete(final K... keys) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {
                return conn.sync().del(keys);
            }
        }

        return 0;
    }

    @Override
    public Future<Long>  deleteNonBlocking(final K... keys) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.async().unlink(keys);
            }
        }

        return ConcurrentUtils.constantFuture(0l);
    }

    @Override
    public String flushAll() {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {
                return conn.sync().flushall();
            }
        }

        return "Error";
    }

    /// HASHES

    @Override
    public
    boolean existsHash (final K key, final K field) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                try {
                    return conn.sync().hexists(key, field);
                } catch (RedisCommandTimeoutException e) {
                    throw new CacheTimeoutException(e);
                }
            }
        }

        return false;
    }

    @Override
    public V getHash(final K key, final K field) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                try {
                    return conn.sync().hget(key, field);
                } catch (RedisCommandTimeoutException e) {
                    throw new CacheTimeoutException(e);
                }
            }
        }

        return null;
    }

    @Override
    public Map<K, V> getHash(final K key) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                try {
                    return conn.sync().hgetall(key);
                } catch (RedisCommandTimeoutException e) {
                    throw new CacheTimeoutException(e);
                }
            }
        }

        return Collections.emptyMap();
    }

    @Override
    public Set<K> fieldsHash (final K key) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                try {
                    return new LinkedHashSet<>(conn.sync().hkeys(key));
                } catch (RedisCommandTimeoutException e) {
                    throw new CacheTimeoutException(e);
                }
            }
        }

        return Collections.emptySet();
    }

    @Override
    public List<Map.Entry<K, V>> getHash(final K key, final K... fields) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                try {
                    return conn.sync().hmget(key, fields).stream()
                            .map(kvKeyValue ->
                                    CollectionsUtils.entry(
                                            kvKeyValue.getKey(), kvKeyValue.getValue()))
                            .collect(CollectionsUtils.toImmutableList());
                } catch (RedisCommandTimeoutException e) {
                    throw new CacheTimeoutException(e);
                }
            }
        }

        return Collections.emptyList();
    }

    @Override
    public SetResult setHash(final K key, final Map<K, V> map) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().hset(key, map) == map.size()?
                        SetResult.SUCCESS: SetResult.FAIL;
            } else {
                return SetResult.NO_CONN;
            }
        }
    }

    @Override
    public SetResult setHash(final K key, final K field, final V value) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().hset(key, field, value)?
                        SetResult.SUCCESS: SetResult.FAIL;
            } else {
                return SetResult.NO_CONN;
            }
        }
    }

    @Override
    public long deleteHash(final K key, K... fields) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().hdel(key, fields);
            } else {
                return -1;
            }
        }
    }

    /// Incr

    @Override
    public long incrementOne(K key) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().incr(key);
            } else {
                return -1;
            }
        }
    }

    @Override
    public long increment(K key, long amount) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().incrby(key, amount);
            } else {
                return -1;
            }
        }
    }

    @Override
    public Future<Long> incrementOneAsync (final K key) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.async().incr(key);
            } else {
                return ConcurrentUtils.constantFuture(-1L);
            }
        }

    }

    @Override
    public Future<Long> incrementAsync (final K key, final long amount) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.async().incrby(key, amount);
            } else {
                return ConcurrentUtils.constantFuture(-1L);
            }
        }
    }

    @Override
    public long getIncrement (final K key) {

        try (StatefulRedisConnection<K,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                final Command<K, V, V> get =
                        new Command<K, V, V>(CommandType.GET,
                                new ValueOutput<K, V>(this.codec),
                                new CommandArgs<>(this.codec).addKey(key));

                conn.dispatch(get);

                return ConversionUtils.toLong(get.get(), -1l);
            } else {
                return -1L;
            }
        }
    }
    //////////

    private GenericObjectPool<StatefulRedisConnection<K, V>> buildPool() {

        //todo: we have to have a mechanism when the connection is wrong on a bad space, to remove it from the pool and create a new one
        final GenericObjectPoolConfig config = new GenericObjectPoolConfig();

        config.setTestOnBorrow(true);
        config.setMinEvictableIdleTimeMillis(TimeUnit.MINUTES.toMillis(5));

        config.setMinIdle(this.minIdleConnections);
        config.setMaxTotal(this.maxConnections);

        final DefaultClientResources clientResources = // Does not cache DNS lookups
                        DefaultClientResources.builder().dnsResolver(new DirContextDnsResolver()).build();

        final io.lettuce.core.RedisClient lettuceClient = io.lettuce.core.RedisClient.create(clientResources);

        if (redisUris.size() == 1) { // only one node

            return ConnectionPoolSupport.createGenericObjectPool(() -> {

                try {

                    final StatefulRedisConnection<K, V> connection =
                            lettuceClient.connect(codec, redisUris.get(0));

                    if (timeout > 0) {
                        connection.setTimeout(Duration.ofMillis(timeout));
                    }

                    return connection;
                } catch (Exception e) {

                    Logger.warnAndDebug(this.getClass(), e);
                    throw new DotStateException(e);
                }
            }, config, true);
        } else {

            return ConnectionPoolSupport.createGenericObjectPool(() -> {

                try {

                    final StatefulRedisConnection<K, V> connection =
                            MasterReplica
                                    .connect(lettuceClient,
                                            // todo: remove this in favor of Snappy compressor
                                            // make this configurable in order to use to use gzip or snappy
                                            codec,
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

}
