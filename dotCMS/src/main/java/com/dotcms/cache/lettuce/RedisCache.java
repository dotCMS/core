package com.dotcms.cache.lettuce;

import com.dotcms.util.DotCloneable;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Redis Cache implementation Notes: 1) This redis implementation does not have the clusted id, b/c redis handles each
 * member, key, hash, incr and channel by using the cluster id so it is not need to repeat it here.
 * <p>
 * The cache handles a ttl, one global by default REDIS_SERVER_DEFAULT_TTL, but can prefix a group to the same key in
 * order to have a specific ttl for a section.
 * <p>
 * 2) When a call is running on transaction to avoid dirty saves or fetches, the cache does not put or retrieve
 * anything.
 * <p>
 * 3) Since the information is being stored as a java byte (binary) only can use serializable values, non-serializable
 * objects will be skipped from the cache.
 * <p>
 * 4) Objects that implements {@link DotCloneable}, the cache will returns a Clone of the object stored on the cache
 * instead of the actual copy on the cache this will helps
 */
public class RedisCache extends CacheProvider {

    private static final long serialVersionUID = -855583393078878276L;

    private static final Long ZERO = 0L;

    protected final String REDIS_GROUP_KEY;
    protected final String REDIS_PREFIX_KEY;
    private final Lazy<RedisClient<String, Object>> client;

    private final int keyBatchingSize = Config.getIntProperty("REDIS_SERVER_KEY_BATCH_SIZE", 1000);
    private final long defaultTTL = Config.getLongProperty("REDIS_SERVER_DEFAULT_TTL", -1);
    private final Map<String, Long> groupTTLMap = new ConcurrentHashMap<>();

    public RedisCache(final Lazy<RedisClient<String, Object>> client) {

        this.client = client;
        this.REDIS_GROUP_KEY = "GROUP_KEY";
        this.REDIS_PREFIX_KEY = "PREFIX_KEY";
    }

    public RedisCache() {
        this(Lazy.of(() -> RedisClientFactory.getClient("cache")));
    }

    @Override
    public String getName() {
        return "Redis Provider";
    }

    @Override
    public String getKey() {
        return "Redis";
    }

    @Override
    public boolean isDistributed() {
        return true;
    }

    protected RedisClient<String, Object> getClient() {

        return client.get();
    }

    /**
     * returns a cache key
     *
     * @param group
     * @param key
     * @return
     */
    @VisibleForTesting
    String cacheKey(final String group, final String key) {
        return this.REDIS_PREFIX_KEY
                + "." +
                (
                        group != null && key != null
                                ? group + "." + key
                                : group != null
                                ? group
                                : ""
                )
                + ".";
    }

    @VisibleForTesting
    String cacheKey(final String group) {
        return cacheKey(group, null);
    }

    @Override
    public void init() {

        Logger.info(this.getClass(), "*** Initializing [" + getName() + "].");
        Logger.info(this.getClass(), "          prefix [" + this.REDIS_PREFIX_KEY + "]");
        Logger.info(this.getClass(), "          inited [" + this.isInitialized() + "]");
        Logger.info(this.getClass(), "*** Initialized  [" + getName() + "].");
    }

    @Override
    public boolean isInitialized() {

        return Try.of(() -> this.client.get().ping()).onFailure(e -> Logger.warn(RedisCache.class, e.getMessage()))
                .getOrElse(false);
    }

    @Override
    public void put(final String group, final String key, final Object content) {

        // this avoid mutability and dirty cache issues
        if (DbConnectionFactory.inTransaction()) {
            Logger.debug(this, () -> "In Transaction, Skipping the put to Redis cache for group: "
                    + group + "key: " + key);
            return;
        }
        if (key == null || group == null || !(content instanceof Serializable)) {
            Logger.debug(this, () -> "The content: " + (null != content ? content.getClass() : "unknown") +
                    " is not serialize, Skipping the put to Redis cache for group: "
                    + group + "key: " + key);
            return;
        }

        Logger.debug(this, () -> "Redis, putting group: " + group + "key" + key);
        final long ttl = this.getTTL(group);
        final String cacheKey = this.cacheKey(group, key);
        final Future<String> future = this.getClient().setAsync(cacheKey, content, ttl);
        this.getClient().addAsyncMembers(REDIS_GROUP_KEY, group);
        if (Logger.isDebugEnabled(this.getClass())) {

            String msg = "Error";
            try {
                msg = future.get();
            } catch (InterruptedException | ExecutionException e) {
                msg = e.getMessage();
            }
            if (!"OK".equalsIgnoreCase(msg)) {
                Logger.debug(this, "Redis, putting group: " + group +
                        "key" + key + "result: " + msg);
            }
        }


    }

    private long getTTL(final String group) {

        // try to figured out if any time out by group, otherwise uses the default ttl
        final String groupTTLKey = "REDIS_SERVER_DEFAULT_TTL_" + group;
        final long ttl = this.groupTTLMap.computeIfAbsent(groupTTLKey,
                k -> Config.getLongProperty(groupTTLKey, -1));
        return -1 == ttl ? this.defaultTTL : ttl;
    }

    @Override
    public Object get(final String group, final String key) {

        // this avoid mutability and dirty cache issues
        if (DbConnectionFactory.inTransaction()) {

            Logger.debug(this, () -> "In Transaction, Skipping the get to Redis cache for group: "
                    + group + "key" + key);
            return null;
        }

        if (key == null || group == null) {
            return null;
        }

        final String cacheKey = this.cacheKey(group, key);
        try {

            return this.extractObject(this.getClient().get(cacheKey));
        } catch (Exception e) {

            Logger.debug(this, "Timeout error on getting Redis cache for group: "
                    + group + "key" + key + " msg: " + e.getMessage());
            return null;
        }

    }

    private Object extractObject(final Object o) {
        return o != null && o instanceof DotCloneable ?
                this.extractObject(DotCloneable.class.cast(o)) : o;
    }

    private Object extractObject(final DotCloneable o) {

        return Try.of(() -> o.clone()).getOrElse(o);
    }


    /**
     * removes cache keys async and resets the get timer that reenables get functions
     *
     * @param keys
     */
    private void removeKeys(final String... keys) {

        if (UtilMethods.isSet(keys)) {

            this.getClient().deleteNonBlocking(keys);
        }
    }

    @Override
    public void remove(final String group, final String key) {

        final String cacheKey = this.cacheKey(group, key);
        this.removeKeys(cacheKey);
    }

    @Override
    public void remove(final String group) {

        if (!UtilMethods.isEmpty(group)) {

            // No leading "*": deleteFromPattern prepends the cluster prefix so the scan stays scoped to THIS
            // client's keys. A leading "*" would match (and delete) other clusters' keys in a shared Redis space.
            final String pattern = cacheKey(group) + StringPool.STAR;
            this.getClient().deleteFromPattern(pattern);
        }
    }

    @Override
    public void removeAll() {

        // cluster-scoped by deleteFromPattern (keyPrefix); no leading "*" so we never touch other clusters' keys
        final String pattern = this.REDIS_PREFIX_KEY + "." + StringPool.STAR;
        this.getClient().deleteFromPattern(pattern);
    }

    @Override
    public Set<String> getKeys(final String group) {

        final String prefix = this.cacheKey(group);
        final String matchesPattern = prefix + StringPool.STAR;
        final Set<String> keys = new LinkedHashSet<>();
        this.getClient().scanKeys(matchesPattern, this.keyBatchingSize,
                redisKeys -> redisKeys.stream()
                        // strip only the leading group prefix to recover the real key (replace() is global)
                        .map(redisKey -> redisKey.startsWith(prefix)
                                ? redisKey.substring(prefix.length())
                                : redisKey)
                        .forEach(keys::add));

        return keys;
    }


    @VisibleForTesting
    Set<String> getAllKeys() {

        return getKeys(null);
    }


    /**
     * returns the number of cache keys in any given group
     *
     * @param group
     * @return
     */
    private long keyCount(final String group) {

        // Never throw: getStats() aggregates per-provider and a thrown exception here makes
        // CacheProviderAPIImpl.getStats() drop the entire Redis provider from the cache stats screen.
        try {
            final RedisClient<String, Object> redisClient = this.getClient();
            final String prefix = LettuceAdapter.getMasterReplicaLettuceClient(redisClient)
                    .wrapKey(this.cacheKey(group) + StringPool.STAR);
            final String script = "return #redis.pcall('keys', '" + prefix + "')";

            try (StatefulRedisConnection<String, Object> conn =
                         LettuceAdapter.getStatefulRedisConnection(redisClient)) {

                // conn can be null when the pool fails to borrow a connection
                if (null != conn && conn.isOpen()) {

                    final Object keyCount = conn.sync().eval(script, ScriptOutputType.INTEGER, "0");
                    return keyCount instanceof Long ? (Long) keyCount : ZERO;
                }
            }
        } catch (final Exception e) {

            Logger.warnAndDebug(RedisCache.class,
                    "Unable to count Redis keys for group '" + group + "': " + e.getMessage(), e);
        }

        return ZERO;
    }


    @Override
    public Set<String> getGroups() {

        return this.getClient().getMembers(REDIS_GROUP_KEY).stream()
                .map(k -> k.toString()).collect(Collectors.toSet());
    }

    @Override
    public CacheProviderStats getStats() {

        final CacheStats providerStats = new CacheStats();
        final CacheProviderStats cacheProviderStats = new CacheProviderStats(providerStats, getName());

        final NumberFormat nf = DecimalFormat.getInstance();
        // Getting the list of groups. Guard against a throw: CacheProviderAPIImpl.getStats() drops the whole
        // provider from the cache stats screen if getStats() throws, so the Redis region would vanish entirely.
        Set<String> currentGroups;
        try {
            currentGroups = getGroups();
        } catch (final Exception e) {
            Logger.warnAndDebug(RedisCache.class, "Unable to list Redis groups: " + e.getMessage(), e);
            currentGroups = Collections.emptySet();
        }

        for (final String group : currentGroups) {

            final CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, "dotCMS: " + group);
            stats.addStat(CacheStats.REGION_SIZE, nf.format(keyCount(group)));

            cacheProviderStats.addStatRecord(stats);
        }

        // Always emit at least one record; otherwise the cache stats screen renders an empty/blank table
        // (no columns, no rows) for this provider. Mirrors CaffineCache's empty-groups fallback.
        if (currentGroups.isEmpty()) {

            final CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, "n/a");
            stats.addStat(CacheStats.REGION_SIZE, 0);
            cacheProviderStats.addStatRecord(stats);
        }

        return cacheProviderStats;
    }

    @Override
    public void shutdown() {

        Logger.info(this.getClass(), "*** Shutdown [" + getName() + "] .");

    }

}
