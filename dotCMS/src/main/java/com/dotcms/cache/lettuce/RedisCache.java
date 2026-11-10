package com.dotcms.cache.lettuce;

import com.dotcms.util.DotCloneable;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
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
        return o instanceof DotCloneable cloneable ? this.extractObject(cloneable) : o;
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
        // also clear the group registry SET; otherwise getGroups() (and getStats()) keep returning every group
        // ever used after a full flush, growing unbounded and making stats do O(groups) pooled SCANs
        this.getClient().deleteNonBlocking(REDIS_GROUP_KEY);
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


    /** Counts every known group prefix in one cluster-scoped scan without retaining the keyspace. */
    private Map<String, Long> keyCounts(final Set<String> groups, final CacheProviderStats collection) {
        final Map<String, Long> counts = groups.stream()
                .collect(Collectors.toMap(this::cacheKey, group -> 0L));
        if (counts.isEmpty()) {
            return counts;
        }
        try {
            this.getClient().scanKeys(this.REDIS_PREFIX_KEY + "." + StringPool.STAR,
                    this.keyBatchingSize, keys -> {
                        for (final String key : keys) {
                            // Preserve prefix matching for dotted/overlapping group names.
                            for (int dot = key.indexOf('.', this.REDIS_PREFIX_KEY.length() + 1);
                                    dot >= 0; dot = key.indexOf('.', dot + 1)) {
                                counts.computeIfPresent(key.substring(0, dot + 1),
                                        (prefix, count) -> count + 1);
                            }
                        }
                    });
        } catch (final Exception e) {
            // Preserve UI rows, but do not publish partial counts as complete metrics.
            collection.markUnavailable();
            Logger.warnAndDebug(RedisCache.class, "Unable to count Redis cache keys: " + e.getMessage(), e);
        }
        return counts;
    }


    @Override
    public Set<String> getGroups() {

        return this.getClient().getMembers(REDIS_GROUP_KEY).stream()
                .filter(Objects::nonNull)
                .map(Object::toString).collect(Collectors.toSet());
    }

    /** Reports region sizes from a single streamed keyspace scan, retaining failure/empty-provider rows. */
    @Override
    public CacheProviderStats getStats() {

        final CacheStats providerStats = new CacheStats();
        final CacheProviderStats cacheProviderStats = new CacheProviderStats(providerStats, getName());

        final NumberFormat nf = DecimalFormat.getInstance();
        // Keep the legacy stats-screen rows on discovery failure; metrics use availability metadata.
        Set<String> currentGroups;
        try {
            if (getClient() instanceof NullLettuceClient<?, ?>) {
                throw new DotStateException("Redis client unavailable for cache statistics");
            }
            currentGroups = getGroups();
        } catch (final Exception e) {
            Logger.warnAndDebug(RedisCache.class, "Unable to list Redis groups: " + e.getMessage(), e);
            cacheProviderStats.markUnavailable();
            currentGroups = Collections.emptySet();
        }

        final Map<String, Long> counts = keyCounts(currentGroups, cacheProviderStats);
        for (final String group : currentGroups) {

            final CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, "dotCMS: " + group);
            stats.addStat(CacheStats.REGION_SIZE, nf.format(counts.get(this.cacheKey(group))));

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
