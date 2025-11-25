package com.dotcms.cache.lettuce;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.Converter;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.EnvironmentVariablesService;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
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
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.output.ValueOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DirContextDnsResolver;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.BLANK;
import static io.lettuce.core.ScriptOutputType.STATUS;

/**
 * Master replica implementation of redis cache. It works as a replicator when there is more than 1 URIs as part of the
 * {@code REDIS_LETTUCECLIENT_URLS} config. This implementation wraps keys, members and channels by prefixing them with
 * the cluster id -- usually the shortified version of the ID. This helps to avoid collisions on ghetto Redis cluster
 * implementation where can live 2 or more clients into the same Redis space. It handles the connections for GET, PUT,
 * INC and hashes by pool, and you can configure it via the following properties:
 * <ul>
 *     <li>{@code REDIS_LETTUCECLIENT_TIMEOUT_MS}: Timeout (3000 by default).</li>
 *     <li>{@code REDIS_LETTUCECLIENT_MIN_IDLE_CONNECTIONS}: Min connections to redis alive when idle (2 by default).
 *     </li>
 *     <li>{@code REDIS_LETTUCECLIENT_MAX_CONNECTIONS}: Max connections at all (5 by default).</li>
 * </ul>
 * <p>You can create as many clients as you need. By default, the empty constructor uses the {@link DotObjectCodec}
 * which is a GZIP, Java serialized byte code. In addition, internally, the keys, channels, member names, etc are
 * handled as Strings. However, you can use whatever you want as a key, but a converter to transform the key to String
 * and vice-versa must be provided. In case the key (the most normal case) is a String, you do not need to implement
 * anything.</p>
 * <p>In case you need to use the Redis connection, you can get it via the
 * {@link MasterReplicaLettuceClient#getConnection()} method. You can use the {@link LettuceAdapter} in order to
 * transform the connection to a Lettuce connection. In case the implementation is run by another library, it would need
 * another adapter. By default, dotCMS uses the Lettuce Client to connect to Redis.</p>
 * <p><b>IMPORTANT:</b> When the Tomcat Redis Session Manager is enabled, you can skip the Lettuce Client configuration
 * and fall back to using the plugin's configuration instead. This will avoid having two separate Redis setups for the
 * same dotCMS environment.</p>
 *
 * @param <K> The Class representing the key used to store data in Redis. Usually a String.
 * @param <V> The Class representing the value stored in Redis. Usually an Object.
 * @author jsanca
 */
public class MasterReplicaLettuceClient<K, V> implements RedisClient<K, V> {

    public static final String REDIS_SESSION_ENABLED_PROP = "TOMCAT_REDIS_SESSION_ENABLED";
    public static final String HOST_PROP = "TOMCAT_REDIS_SESSION_HOST";
    public static final String PORT_PROP = "TOMCAT_REDIS_SESSION_PORT";
    public static final String USERNAME_PROP = "TOMCAT_REDIS_SESSION_USERNAME";
    public static final String PASSWORD_PROP = "TOMCAT_REDIS_SESSION_PASSWORD";
    public static final String SSL_ENABLED_PROP = "TOMCAT_REDIS_SESSION_SSL_ENABLED";
    public static final String DATABASE_PROP = "TOMCAT_REDIS_SESSION_DATABASE";
    public static final String TIMEOUT_PROP = "TOMCAT_REDIS_SESSION_TIMEOUT";

    private static final String OK_RESPONSE = "OK";
    private static final String ERROR_RESPONSE = "ERROR";

    private final List<RedisURI> redisUris = this.createRedisConnection();
    private final GenericObjectPool<StatefulRedisConnection<String, V>> pool;
    private final RedisCodec<String, V> codec;
    private final String clusterId;
    private final io.lettuce.core.RedisClient lettuceClient;
    private final Converter<K, String> keyToStringConverter;
    private final Converter<String, K> stringToKeyConverter;

    public MasterReplicaLettuceClient() {

        this(CompressionCodec.valueCompressor(new DotObjectCodec(),
                CompressionCodec.CompressionType.GZIP),
                ClusterFactory.getClusterId());
    }

    public MasterReplicaLettuceClient(final RedisCodec<String, V> codec, final String clusterId) {

        this (codec, clusterId, k -> k.toString(), s -> (K)s);
    }

    public MasterReplicaLettuceClient(final RedisCodec<String, V> codec, final String clusterId,
                                      final Converter<K, String> keyToStringConverter,
                                      final Converter<String, K> stringToKeyConverter) {

        this.clusterId = clusterId;
        this.codec     = codec;
        this.keyToStringConverter = keyToStringConverter;
        this.stringToKeyConverter = stringToKeyConverter;
        DefaultClientResources clientResources = // Does not cache DNS lookups
                DefaultClientResources.builder().dnsResolver(new DirContextDnsResolver()).build();
        this.lettuceClient = io.lettuce.core.RedisClient.create(clientResources);
        this.pool = buildPool();
    }

    /**
     * Creates the appropriate connection objects used by dotCMS to access the Redis server. The connection is created
     * based on a fallback mechanism:
     * <ul>
     *     <li>If the {@code REDIS_LETTUCECLIENT_URLS} property -- which allows to set one or more Redis servers -- is
     *     set, dotCMS will create the Lettuce Clients based on such a configuration.</li>
     *     <li>If the Lettuce Client configuration is not set, but the Tomcat Redis Session Manager plugin is activated
     *     -- via the {@link #REDIS_SESSION_ENABLED_PROP} property -- dotCMS will create the it based on the existing
     *     configuration set in the plugin, as a fallback.</li>
     * </ul>
     * For more information, please refer to the list of available configuration parameters in the
     * <a href="https://github.com/dotCMS/tomcat-redis-session-manager/blob/trunk/README.markdown">Redis Session Manager
     * repository</a>
     *
     * @return The list with one or more {@link RedisURI} objects representing the Redis servers that dotCMS will
     * connect to.
     */
    protected List<RedisURI> createRedisConnection() {
        final EnvironmentVariablesService envVarService = EnvironmentVariablesService.getInstance();
        final List<String> lettuceUrls = Arrays.asList(Config.getStringArrayProperty("REDIS_LETTUCECLIENT_URLS", new String[]{}));
        if (!lettuceUrls.isEmpty()) {
            return lettuceUrls.stream()
                    .map(RedisURI::create)
                    .collect(Collectors.toList());
        }
        final String redisSessionEnabled = envVarService.getenv().getOrDefault(REDIS_SESSION_ENABLED_PROP, "false");
        if (Boolean.parseBoolean(redisSessionEnabled)) {
            final RedisURI.Builder builder = RedisURI.builder()
                    .withHost(envVarService.getenv().getOrDefault(HOST_PROP, "localhost"))
                    .withPort(Integer.parseInt(envVarService.getenv().getOrDefault(PORT_PROP, "6379")));
            if (envVarService.getenv().get(USERNAME_PROP) != null) {
                builder.withAuthentication(
                        envVarService.getenv().getOrDefault(USERNAME_PROP, BLANK),
                        envVarService.getenv().getOrDefault(PASSWORD_PROP, BLANK).toCharArray());
            } else {
                builder.withPassword(envVarService.getenv().getOrDefault(PASSWORD_PROP, BLANK).toCharArray());
            }
            builder.withSsl(Boolean.parseBoolean(envVarService.getenv().getOrDefault(SSL_ENABLED_PROP, "false")))
                    .withDatabase(Config.getIntProperty(DATABASE_PROP, 0))
                    .withTimeout(Duration.ofMillis(Integer.parseInt(envVarService.getenv().getOrDefault(TIMEOUT_PROP, "2000"))));
            return List.of(builder.build());
        }
        return List.of(RedisURI.create("redis://localhost"));
    }

    /**
     * Returns the key prefix (clusterid_)
     * @return String
     */
    protected String keyPrefix () {

        return this.clusterId + StringPool.UNDERLINE;
    }

    /**
     * Wrap a key
     * @param key
     * @return
     */
    protected String wrapKey (final K key) {

        return keyPrefix() + this.keyToStringConverter.convert(key);
    }

    /**
     * Un Wrap a key
     * @param key
     * @return
     */
    protected K unwrapKey(final String key) {

        return  this.stringToKeyConverter.convert(key.replace(keyPrefix(), BLANK));
    }
    /**
     * Returns a StatefulRedisConnection
     * @return StatefulRedisConnection
     */
    protected StatefulRedisConnection<String, V> getConn() {
        return Try.of(() -> pool.borrowObject()).onFailure(
                        e -> Logger.warnAndDebug(MasterReplicaLettuceClient.class, "redis unable to connect: " + e, e))
                        .getOrNull();

    }

    @Override
    public Object getConnection () {

        return this.getConn();
    }

    /**
     * Returns a StatefulRedisPubSubConnection
     * @return StatefulRedisPubSubConnection
     */
    protected StatefulRedisPubSubConnection<String, V> getPubSubConn() {

        return this.lettuceClient.connectPubSub(codec, redisUris.get(0));
    }

    @Override
    public boolean isOpen (final StatefulConnection connection) {

        return null != connection && connection.isOpen();
    }

    @Override
    public boolean ping () {

        boolean validPing = false;
        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

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

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().echo(msg);
            }
        }

        return null;
    }

    @Override
    public SetResult set (final K key, final V value) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return OK_RESPONSE.equalsIgnoreCase(conn.sync().set(this.wrapKey(key), value))?
                        SetResult.SUCCESS: SetResult.FAIL;
            } else {
                return SetResult.NO_CONN;
            }
        }
    }

    @Override
    public Future<String> setAsync (final K key, final V value) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.async().set(this.wrapKey(key), value);
            }
        }

        return ConcurrentUtils.constantFuture(ERROR_RESPONSE);
    }

    @Override
    public long addMembers (final K key, final V... values) {

        long membersCount = 0;

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                membersCount = conn.sync().sadd(this.wrapKey(key), values);
            }
        }

        return membersCount;
    }

    @Override
    public Future<Long> addAsyncMembers (final K key, final V... values) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.async().sadd(this.wrapKey(key), values);
            }
        }

        return ConcurrentUtils.constantFuture(0l);
    }

    @Override
    public void set (final K key, final V value, final long ttlMillis) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                conn.sync().set(this.wrapKey(key), value,
                        SetArgs.Builder.px(ttlMillis));
            }
        }
    }

    @Override
    public Future<String> setAsync (final K key, final V value, final long ttlMillis) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return ttlMillis == -1?
                        conn.async().set(this.wrapKey(key), value):
                        conn.async().set(this.wrapKey(key), value, SetArgs.Builder.px(ttlMillis));
            }
        }

        return ConcurrentUtils.constantFuture(ERROR_RESPONSE);
    }

    @Override
    public SetResult setIfAbsent (final K key, final V value) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            return  this.isOpen(conn)?
                    (
                            conn.sync().setnx(this.wrapKey(key), value)?
                                SetResult.SUCCESS: SetResult.FAIL
                    ): SetResult.NO_CONN;
        }
    }

    @Override
    public V setIfPresent (final K key, final V value) {

        V valueToReturn = null;

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                if(OK_RESPONSE.equalsIgnoreCase(conn.sync().set(this.wrapKey(key), value,
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

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                ttlMillis = conn.sync().pttl(this.wrapKey(key));
            }
        }

        return ttlMillis;
    }

    @Override
    public V get(final K key) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                try {
                    return conn.sync().get(this.wrapKey(key));
                } catch (RedisCommandTimeoutException e) {
                        throw new CacheTimeoutException(e);
                }
            }
        }

        return null;
    }

    @Override
    public Set<V> getMembers (final K key) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().smembers(this.wrapKey(key));
            }
        }

        return Collections.emptySet();
    }

    @Override
    public void scanEachKey(final String matchesPattern, int keyBatchingSize, final Consumer<K> keyConsumer) {

        KeyScanCursor<String> scanCursor = null;

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (isOpen(conn)) {

                final RedisCommands<String, V> syncCommand = conn.sync();
                final ScanArgs scanArgs = ScanArgs.Builder.matches(this.keyPrefix() + matchesPattern).limit(keyBatchingSize);
                do {

                    scanCursor = scanCursor == null?
                            syncCommand.scan(scanArgs):syncCommand.scan(scanCursor, scanArgs);

                    scanCursor.getKeys().forEach(key ->
                            keyConsumer.accept(this.unwrapKey(key)));
                } while (!scanCursor.isFinished());
            }
        }
    }

    public void scanKeys(final String matchesPattern, int keyBatchingSize,
                         final Consumer<Collection<K>> keyConsumer) {

        KeyScanCursor<String> scanCursor = null;

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (isOpen(conn)) {

                final RedisCommands<String, V> syncCommand = conn.sync();
                final ScanArgs scanArgs = ScanArgs.Builder.matches(this.keyPrefix() + matchesPattern).limit(keyBatchingSize);
                do {

                    scanCursor = scanCursor == null?
                            syncCommand.scan(scanArgs):syncCommand.scan(scanCursor, scanArgs);

                    keyConsumer.accept(ConversionUtils.INSTANCE.convert(
                            scanCursor.getKeys(), this::unwrapKey));
                } while (!scanCursor.isFinished());
            }
        }
    }

    @Override
    public V delete(final K key) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {
                return conn.sync().getdel(this.wrapKey(key));
            }
        }

        return null;
    }

    @Override
    public long delete(final K... keys) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {
                return conn.sync().del(ConversionUtils.INSTANCE.convertToArray(this.keyToStringConverter, String.class, keys));
            }
        }

        return 0;
    }

    @Override
    public Future<Long>  deleteNonBlocking(final K... keys) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.async().unlink(ConversionUtils.INSTANCE.convertToArray(this::wrapKey, String.class, keys));
            }
        }

        return ConcurrentUtils.constantFuture(0l);
    }

    /// HASHES

    @Override
    public
    boolean existsHash (final K key, final K field) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                try {
                    return conn.sync().hexists(this.wrapKey(key), this.keyToStringConverter.convert(field));
                } catch (RedisCommandTimeoutException e) {
                    throw new CacheTimeoutException(e);
                }
            }
        }

        return false;
    }

    @Override
    public V getHash(final K key, final K field) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                try {
                    return conn.sync().hget(this.wrapKey(key), this.keyToStringConverter.convert(field));
                } catch (RedisCommandTimeoutException e) {
                    throw new CacheTimeoutException(e);
                }
            }
        }

        return null;
    }

    @Override
    public Map<K, V> getHash(final K key) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                try {
                    final Map<String, V> intermedialMap = conn.sync().hgetall(this.wrapKey(key));
                    if (UtilMethods.isSet(intermedialMap)) {

                        final ImmutableMap.Builder<K, V> mapBuilder = new ImmutableMap.Builder<>();

                        for (Map.Entry<String, V> entry : intermedialMap.entrySet()) {

                            mapBuilder.put(this.stringToKeyConverter.convert(entry.getKey()), entry.getValue());
                        }

                        return mapBuilder.build();
                    }
                } catch (RedisCommandTimeoutException e) {
                    throw new CacheTimeoutException(e);
                }
            }
        }

        return Collections.emptyMap();
    }

    @Override
    public Set<K> fieldsHash (final K key) {

        try (StatefulRedisConnection<String, V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                try {
                    return conn.sync().hkeys(this.wrapKey(key)).stream().map(this.stringToKeyConverter::convert)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                } catch (RedisCommandTimeoutException e) {
                    throw new CacheTimeoutException(e);
                }
            }
        }

        return Collections.emptySet();
    }

    @Override
    public List<Map.Entry<K, V>> getHash(final K key, final K... fields) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                try {
                    return conn.sync().hmget(this.wrapKey(key),
                            ConversionUtils.INSTANCE.convertToArray(this.keyToStringConverter, String.class, fields)).stream()
                            .map(kvKeyValue ->
                                    CollectionsUtils.entry(
                                            this.stringToKeyConverter.convert(kvKeyValue.getKey()), kvKeyValue.getValue()))
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

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                final ImmutableMap.Builder<String, V> mapBuilder = new ImmutableMap.Builder<>();

                for (Map.Entry<K, V> entry : map.entrySet()) {

                    mapBuilder.put(this.keyToStringConverter.convert(entry.getKey()), entry.getValue());
                }
                return conn.sync().hset(this.wrapKey(key), mapBuilder.build()) == map.size()?
                        SetResult.SUCCESS: SetResult.FAIL;
            } else {
                return SetResult.NO_CONN;
            }
        }
    }

    @Override
    public SetResult setHash(final K key, final K field, final V value) {

        try (StatefulRedisConnection<String, V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().hset(this.wrapKey(key),
                        this.keyToStringConverter.convert(field), value)?
                        SetResult.SUCCESS: SetResult.FAIL;
            } else {
                return SetResult.NO_CONN;
            }
        }
    }

    @Override
    public long deleteHash(final K key, K... fields) {

        try (StatefulRedisConnection<String, V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().hdel(this.wrapKey(key),
                        ConversionUtils.INSTANCE.convertToArray(this.keyToStringConverter, String.class, fields));
            } else {
                return -1;
            }
        }
    }

    /// Incr

    @Override
    public long incrementOne(K key) {

        try (StatefulRedisConnection<String, V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().incr(this.wrapKey(key));
            } else {
                return -1;
            }
        }
    }

    @Override
    public long increment(K key, long amount) {

        try (StatefulRedisConnection<String, V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.sync().incrby(this.wrapKey(key), amount);
            } else {
                return -1;
            }
        }
    }

    @Override
    public Future<Long> incrementOneAsync (final K key) {

        try (StatefulRedisConnection<String, V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.async().incr(this.wrapKey(key));
            } else {
                return ConcurrentUtils.constantFuture(-1L);
            }
        }

    }

    @Override
    public Future<Long> incrementAsync (final K key, final long amount) {

        try (StatefulRedisConnection<String, V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                return conn.async().incrby(this.wrapKey(key), amount);
            } else {
                return ConcurrentUtils.constantFuture(-1L);
            }
        }
    }

    @Override
    public long getIncrement (final K key) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                final Command<String, V, V> get =
                        new Command<>(CommandType.GET,
                                new ValueOutput<String, V>(this.codec),
                                new CommandArgs<>(this.codec).addKey(this.wrapKey(key)));

                conn.dispatch(get);

                return ConversionUtils.toLong(get.get(), -1l);
            } else {
                return -1L;
            }
        }
    }

    ////// Streams Pub/Sub
    private final Map<String, Tuple2<DotPubSubListener, StatefulRedisPubSubConnection<String, V>>> channelStatefulRedisPubSubConnectionMap = new ConcurrentHashMap<>();
    private final Map<String, List<String>> channelReferenceMap = new ConcurrentHashMap<>();

    @Override
    public  void subscribe (final Consumer<V> messageConsumer, final K channelIn, final String subscriberInstanceId) {

        final StatefulRedisPubSubConnection<String, V> conn = this.getPubSubConn();
        final String channelUUID = subscriberInstanceId;

        if (this.isOpen(conn)) {

            final String channel = this.wrapKey(channelIn);
            final DotPubSubListener dotPubSubListener = new DotPubSubListener (messageConsumer, channel);
            final RedisPubSubAsyncCommands<String, V> commands = conn.async();
            commands.getStatefulConnection().addListener(dotPubSubListener);
            commands.subscribe(channel);

            final Tuple2<DotPubSubListener, StatefulRedisPubSubConnection<String, V>> channelRedisPubSubAsyncCommandsTuple =
                    Tuple.of(dotPubSubListener, conn);

            this.channelStatefulRedisPubSubConnectionMap.put(channelUUID, channelRedisPubSubAsyncCommandsTuple);
            this.channelReferenceMap.computeIfAbsent(channel, key -> new ArrayList<>()).add(channelUUID);
        }
    }

    @Override
    public String subscribe (final Consumer<V> messageConsumer, final K channelIn) {

        final String channelUUID = UUIDUtil.uuid();
        subscribe(messageConsumer, channelIn, channelUUID);
        return channelUUID;
    }

    @Override
    public  boolean unsubscribeSubscriber(final String subscriberId, final K channelIn) {

        final String channel = this.wrapKey(channelIn);
        boolean successUnSubscription = false;

        if (this.channelReferenceMap.containsKey(channel)) {

            final List<String> channelUUIDList = this.channelReferenceMap.getOrDefault(channel, Collections.emptyList());

            for (final String channelUUID : channelUUIDList) {

                if (channelUUID.equals(subscriberId)) {

                    final Tuple2<DotPubSubListener, StatefulRedisPubSubConnection<String, V>> channelRedisPubSubAsyncCommandsTuple =
                            this.channelStatefulRedisPubSubConnectionMap.get(channelUUID);
                    if (null != channelRedisPubSubAsyncCommandsTuple) {

                        final RedisPubSubAsyncCommands<String, V> commands = channelRedisPubSubAsyncCommandsTuple._2().async();

                        // if this subscriber is the last one, lets remove the channel
                        if (channelUUIDList.size() == 1) {
                            commands.unsubscribe(channel);
                        }

                        this.channelStatefulRedisPubSubConnectionMap.remove(channelUUID);

                        commands.getStatefulConnection().removeListener(channelRedisPubSubAsyncCommandsTuple._1());
                        // lets close it later
                        DotConcurrentFactory.getInstance().getSubmitter().delay(
                                () -> channelRedisPubSubAsyncCommandsTuple._2().close(), 5, TimeUnit.SECONDS);

                        successUnSubscription = true;
                    }
                }
            }

            if (successUnSubscription) {
                channelUUIDList.remove(subscriberId);
                if (channelUUIDList.size() == 0) {

                    this.channelReferenceMap.remove(channel);
                }
            }
        }

        return successUnSubscription;
    }

    @Override
    public  boolean unsubscribeSubscriber(final K channelIn) {

        final String channel = this.wrapKey(channelIn);

        if (this.channelReferenceMap.containsKey(channel)) {

            final List<String> channelUUIDList = this.channelReferenceMap.getOrDefault(channel, Collections.emptyList());

            for (final String channelUUID : channelUUIDList) {

                final Tuple2<DotPubSubListener, StatefulRedisPubSubConnection<String, V>> channelRedisPubSubAsyncCommandsTuple =
                        this.channelStatefulRedisPubSubConnectionMap.get(channelUUID);
                if (null != channelRedisPubSubAsyncCommandsTuple) {

                    final RedisPubSubAsyncCommands<String, V> commands = channelRedisPubSubAsyncCommandsTuple._2().async();
                    commands.unsubscribe(channel);

                    this.channelStatefulRedisPubSubConnectionMap.remove(channelUUID);

                    commands.getStatefulConnection().removeListener(channelRedisPubSubAsyncCommandsTuple._1());
                    // lets close it later
                    DotConcurrentFactory.getInstance().getSubmitter().delay(
                            () -> channelRedisPubSubAsyncCommandsTuple._2().close(), 5, TimeUnit.SECONDS);
                }
            }

            this.channelReferenceMap.remove(channel);
            return true;
        }

        return false;
    }

    @Override
    public Collection<Object> getSubscribers (final K channelIn) {

        final String channel = this.wrapKey(channelIn);
        final List<Object> subscribers = new ArrayList<>();

        if (this.channelReferenceMap.containsKey(channel)) {

            final List<String> channelUUIDList = channelReferenceMap.get(channel);
            for (final String channelUUID : channelUUIDList) {
                final Tuple2<DotPubSubListener, StatefulRedisPubSubConnection<String, V>> channelRedisPubSubAsyncCommandsTuple =
                        channelStatefulRedisPubSubConnectionMap.get(channelUUID);

                subscribers.add(channelRedisPubSubAsyncCommandsTuple._1());
            }
        }

        return subscribers;
    }

    @Override
    public Collection<K> getChannels() {
        return channelReferenceMap.keySet().stream().map(this::unwrapKey).collect(Collectors.toList());
    }

    @Override
    public void deleteFromPattern(final String pattern) {

        try (StatefulRedisConnection<String,V> conn = this.getConn()) {

            if (this.isOpen(conn)) {

                conn.async().eval("return redis.call('del', unpack(redis.call('keys', '" + pattern + "')))",
                        STATUS, new String[0]);
            }
        }
    }

    @Override
    public Future<Long> publishMessage (final V message, final K channelIn) {

        final String  channel = this.wrapKey(channelIn);

        try (StatefulRedisPubSubConnection<String, V> conn = this.getPubSubConn()) {

            if (this.isOpen(conn)) {

                final RedisPubSubAsyncCommands<String, V> commands = conn.async();
                return commands.publish(channel, message);
            } else {

                return ConcurrentUtils.constantFuture(-1L);
            }
        }
    }

    /**
     * Builds the Generic Connection Pool for the provided Redis servers. Its configuration parameters are set based on
     * the following criteria:
     * <ul>
     *     <li>If specific Lettuce Client parameters are set, use them.</li>
     *     <li>If they're not, use the configuration parameters used by the Tomcat Redis Session Manager.</li>
     *     <li>If they're not set either, use default values.</li>
     * </ul>
     *
     * @return The {@link GenericObjectPool} object used by the Redis Cache.
     */
    private GenericObjectPool<StatefulRedisConnection<String, V>> buildPool() {
        //todo: we have to have a mechanism when the connection is wrong on a bad space, to remove it from the pool and create a new one
        final GenericObjectPoolConfig<StatefulRedisConnection<String, V>> config = new GenericObjectPoolConfig<>();
        final int timeout = this.getIntProperty("REDIS_LETTUCECLIENT_TIMEOUT_MS", "TOMCAT_REDIS_SESSION_TIMEOUT", 3000);
        final int minIdleConnections = this.getIntProperty("REDIS_LETTUCECLIENT_MIN_IDLE_CONNECTIONS", "TOMCAT_REDIS_MIN_IDLE_CONNECTIONS", 2);
        final int maxIdleConnections = this.getIntProperty("REDIS_LETTUCECLIENT_MAX_IDLE_CONNECTIONS", "TOMCAT_REDIS_MAX_IDLE_CONNECTIONS", 2);
        final int maxConnections = this.getIntProperty("REDIS_LETTUCECLIENT_MAX_CONNECTIONS", "TOMCAT_REDIS_MAX_CONNECTIONS", 5);
        config.setTestOnBorrow(true);
        config.setMinEvictableIdleTimeMillis(TimeUnit.MINUTES.toMillis(5));
        config.setMinIdle(minIdleConnections);
        config.setMaxIdle(maxIdleConnections);
        config.setMaxTotal(maxConnections);

        GenericObjectPool<StatefulRedisConnection<String, V>> genericPool;
        if (redisUris.size() == 1) { // only one node

            final Supplier<StatefulRedisConnection<String, V>> connectionSupplier = () -> {

                try {

                    final StatefulRedisConnection<String, V> connection =
                            lettuceClient.connect(codec, redisUris.get(0));

                    if (timeout > 0) {
                        connection.setTimeout(Duration.ofMillis(timeout));
                    }

                    return connection;
                } catch (Exception e) {

                    Logger.warnAndDebug(this.getClass(), e);
                    throw new DotStateException(e);
                }
            };

            genericPool = ConnectionPoolSupport.createGenericObjectPool(connectionSupplier, config, true);
        } else {

            final Supplier<StatefulRedisConnection<String, V>> connectionSupplier = () -> {

                try {

                    final StatefulRedisConnection<String, V> connection =
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
            };

            genericPool = ConnectionPoolSupport.createGenericObjectPool(connectionSupplier, config, true);
        }

        return genericPool;
    }

    /**
     * Utility method that returns the value of a specific Environment Variable. It's very important to note that the
     * internal {@code "DOT_"} prefix is automatically prepended to the {@code key} parameter. This method has two
     * fallback options:
     * <ol>
     *     <li>If the requested Environment Variable is not set, then use the default Environment Variable, which DOES
     *     NOT the {@code "DOT_"} prefix set.</li>
     *     <li>If it's not set either, then default value is returned.</li>
     * </ol>
     *
     * @param key          The name of the Environment Variable to be requested.
     * @param defaultKey   The name of the default Environment Variable to be requested in case the {@code key} doesn't
     *                     return any value.
     * @param defaultValue The default value to be returned in case neither {@code key} nor {@code defaultKey} return
     *                     any value.
     * @return The value of the requested Environment Variable, based on the rules described above.
     */
    protected int getIntProperty(final String key, final String defaultKey, final int defaultValue) {
        String value = Config.getStringProperty(key, null);
        if (!UtilMethods.isSet(value)) {
            value = EnvironmentVariablesService.getInstance().getenv().get(defaultKey);
            if (!UtilMethods.isSet(value)) {
                return defaultValue;
            }
        }
        return Integer.parseInt(value);
    }

}
