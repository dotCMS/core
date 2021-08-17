package com.dotcms.cache.lettuce;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.repackage.EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import com.dotcms.util.DotCloneable;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.liferay.util.StringPool;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.control.Try;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Redis Cache implementation
 */
public class RedisCache extends CacheProvider {

    private static final long serialVersionUID = -855583393078878276L;

    private static final Long ZERO = 0L;
    private static final String KEY_DATE_FORMAT = "yyyy-MM-dd_hh:mm:ss.S";
    private static final String PREFIX_UNSET    = "PREFIX_UNSET";

    private final String REDIS_GROUP_KEY;
    private final String REDIS_PREFIX_KEY;
    private final String clusterId;
    private final RedisClient<String, Object> client;

    // todo: check if we need lazy here or not
    private final int  keyBatchingSize = Config.getIntProperty( "REDIS_SERVER_KEY_BATCH_SIZE", 1000);
    private final long defaultTTL      = Config.getLongProperty("REDIS_SERVER_DEFAULT_TTL", -1);
    final static AtomicReference<String> prefixKey = new AtomicReference(PREFIX_UNSET);
    private final Map<String, Long> groupTTLMap    = new ConcurrentHashMap();

    public RedisCache(final RedisClient<String,Object> client, final String clusterId) {

        this.client           = client;
        this.clusterId        = clusterId;
        this.REDIS_GROUP_KEY  = clusterId + "REDIS_GROUP_KEY";
        this.REDIS_PREFIX_KEY = clusterId + "REDIS_PREFIX_KEY";
    }

    public RedisCache() {
        this(RedisClientFactory.getClient("cache"),
                APILocator.getShortyAPI().shortify(ClusterFactory.getClusterId()));
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

    /**
     * all key values that are put into redis are prefixed by a random key. When a flushall is called,
     * this key is cycled, which basically invalidates all cached entries. The prefix key is stored in
     * redis itself so multiple servers in the cluster can read it. When the key is cycled, we send a
     * CYCLE_KEY event to cluster to refresh the key across cluster
     */
    @VisibleForTesting
    String loadPrefix() {

        String key = prefixKey.get();
        if(!PREFIX_UNSET.equals(key)) {
            return key;
        }
        if (PREFIX_UNSET.equals(key)) {
            key = this.loadPrefixFromRedis();
        }
        if (PREFIX_UNSET.equals(key)) {
            key = setOrGet();
        }
        if (PREFIX_UNSET.equals(key)) {
            Logger.warn(this.getClass(), "unable to determine key prefix");
            key = PREFIX_UNSET;
        }
        prefixKey.set(key);
        return key;
    }
    
    String loadPrefixFromRedis() {

        try {

            final String value = (String)this.client.get(REDIS_PREFIX_KEY);
            return null == value? PREFIX_UNSET: value;
        } catch (Exception e) {

            Logger.debug(this.getClass(), ()-> "unable to get prefix:" + e.getMessage());
            return PREFIX_UNSET;
        }
    }

    String generateNewKey() {

        return this.clusterId + StringPool.UNDERLINE +
                new SimpleDateFormat(KEY_DATE_FORMAT).format(new Date());
    }

    /**
     * This will forceably cycle the prefix key, effectivily wiping the cache.
     * 
     * @return
     */
    @VisibleForTesting
    String cycleKey() {

        final String newKey = this.generateNewKey();

        if (SetResult.SUCCESS != this.client.set(REDIS_PREFIX_KEY, newKey)) {
            return PREFIX_UNSET;
        }

        prefixKey.set(newKey);
        return newKey;
    }

    /**
     * This checks if there is a prefix key in redis. If so, it will return the value stored in redis,
     * otherwise, it will set redis to the value of the newKey and return it.
     * 
     * @return
     */
    @VisibleForTesting
    String setOrGet() {

        final String newKey    = this.generateNewKey();
        final SetResult result = client.setIfAbsent(REDIS_PREFIX_KEY, newKey);

        if (SetResult.NO_CONN == result) {
            return PREFIX_UNSET;
        }

        return SetResult.SUCCESS == result?
                newKey : this.loadPrefixFromRedis();
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
        return loadPrefix() +
                        (
                                group != null && key != null ?
                                "." + group + "." + key :
                                      group != null ?
                                                "." + group + "." :
                                                "."
                        );
    }

    @VisibleForTesting
    String cacheKey(final String group) {
        return cacheKey(group, null);
    }

    @Override
    public void init() {

        Logger.info(this.getClass(), "*** Initializing [" + getName() + "].");
        Logger.info(this.getClass(), "          prefix [" + this.loadPrefix() + "]");
        Logger.info(this.getClass(), "          inited [" + this.isInitialized() + "]");
        Logger.info(this.getClass(), "*** Initialized  [" + getName() + "].");
    }

    @Override
    public boolean isInitialized()  {

        return !PREFIX_UNSET.equals(prefixKey.get());
    }

    @Override
    public void put(final String group, final String key, final Object content) {

        // this avoid mutability and dirty cache issues
        if (DbConnectionFactory.inTransaction()) {

            Logger.debug(this, ()-> "In Transaction, Skipping the put to Redis cache for group: "
                    + group + "key: " + key);
        } else if (key != null && group != null) {

            if (content instanceof Serializable) {

                Logger.debug(this, () -> "Redis, putting group: " + group + "key" + key);
                final long ttl = this.getTTL(group);
                final String cacheKey = this.cacheKey(group, key);
                final Future<String> future = this.client.setAsync(cacheKey, content, ttl);
                this.client.addAsyncMembers(REDIS_GROUP_KEY, group);
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
            } else {

                Logger.debug(this, ()-> "The content: " + (null != content?content.getClass():"unknown") +
                        " is not serialize, Skipping the put to Redis cache for group: "
                        + group + "key: " + key );
            }
        }
    }

    private long getTTL (final String group) {

        // try to figured out if any time out by group, otherwise uses the default ttl
        final String groupTTLKey = group + "_REDIS_SERVER_DEFAULT_TTL";
        final long ttl = this.groupTTLMap.computeIfAbsent(groupTTLKey,
                k -> Config.getLongProperty(group + "_REDIS_SERVER_DEFAULT_TTL", -1));
        return -1 == ttl? this.defaultTTL : ttl;
    }

    @Override
    public Object get(final String group, final String key) {

        // this avoid mutability and dirty cache issues
        if (DbConnectionFactory.inTransaction()) {

            Logger.debug(this, ()-> "In Transaction, Skipping the get to Redis cache for group: "
                    + group + "key" + key);
        } else if (null != key && null != group) {

            final String cacheKey = this.cacheKey(group, key);
            try {

                return this.extractObject(this.client.get(cacheKey));
            } catch (CacheTimeoutException e) {

                Logger.debug(this, "Timeout error on getting Redis cache for group: "
                        + group + "key" + key + " msg: " + e.getMessage());
                return null;
            }
        }

        return null;
    }

    private Object extractObject (final Object o) {

        return o != null && o instanceof DotCloneable?
                this.extractObject(DotCloneable.class.cast(o)): o;
    }

    private Object extractObject (final DotCloneable o) {

        return Try.of(()-> o.clone()).getOrElse(o);
    }


    /**
     * removes cache keys async and resets the get timer that reenables get functions
     * 
     * @param keys
     */
    private void removeKeys(final String... keys) {

        if (UtilMethods.isSet(keys)) {

            this.client.deleteNonBlocking(keys);
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

            final String prefix = cacheKey(group) + StringPool.STAR;
            // Getting all the keys for the given groups
            DotConcurrentFactory.getInstance().getSingleSubmitter
                    (CacheWiper.class.getSimpleName()).submit(new CacheWiper(prefix));
        }
    }

    @Override
    public void removeAll() {

        final String prefix = cacheKey(StringPool.STAR);
        this.cycleKey();
        // Getting all the keys for the given groups
        DotConcurrentFactory.getInstance().getSingleSubmitter
                (CacheWiper.class.getSimpleName()).submit(new CacheWiper(prefix));
    }

    @Override
    public Set<String> getKeys(final String group) {

        final String prefix    = this.cacheKey(group);
        final String matchesPattern = prefix  + StringPool.STAR;
        final Set<String> keys = new LinkedHashSet<>();
        this.client.scanKeys(matchesPattern, this.keyBatchingSize, //keys::addAll);
                redisKeys -> redisKeys.stream().map(redisKey ->  // we remove the prefix in order to have the real key
                        redisKey.replace(prefix, StringPool.BLANK)).forEach(keys::add));

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

        final String prefix = this.cacheKey(group) + StringPool.STAR;
        final String script = "return #redis.pcall('keys', '" + prefix + "')";
        Object keyCount     = ZERO;

        try (StatefulRedisConnection<String,Object> conn = LettuceAdapter.getStatefulRedisConnection(this.client)) {

            if (conn.isOpen()) {

                keyCount = conn.sync().eval(script, ScriptOutputType.INTEGER, "0");
            }
        }

        return (Long) keyCount;
    }



    @Override
    public Set<String> getGroups() {

        return this.client.getMembers(REDIS_GROUP_KEY).stream()
                .map(k -> k.toString()).collect(Collectors.toSet());
    }

    @Override
    public CacheProviderStats getStats() {

        final CacheStats providerStats = new CacheStats();
        final CacheProviderStats cacheProviderStats = new CacheProviderStats(providerStats, getName());
        String memoryStats = null;

        try (StatefulRedisConnection<String,Object> conn = LettuceAdapter.getStatefulRedisConnection(client)) {

            if (!conn.isOpen()) {

                return cacheProviderStats;
            }

            memoryStats = conn.sync().info();
        }

        // Read the total memory usage
        final Map<String, String> redis = getRedisProperties(memoryStats);

        for (final Map.Entry<String, String> entry : redis.entrySet()) {

            final CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, "redis: " + entry.getKey());
            stats.addStat(CacheStats.REGION_SIZE, entry.getValue());
            // ret.addStatRecord(stats);
        }

        final NumberFormat nf = DecimalFormat.getInstance();
        // Getting the list of groups
        final Set<String> currentGroups = getGroups();

        for (final String group : currentGroups) {

            final CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, "dotCMS: " + group);
            stats.addStat(CacheStats.REGION_SIZE, nf.format(keyCount(group)));

            cacheProviderStats.addStatRecord(stats);
        }

        return cacheProviderStats;
    }

    @Override
    public void shutdown() {

        Logger.info(this.getClass(), "*** Shutdown [" + getName() + "] .");
        prefixKey.set(PREFIX_UNSET);
    }

    /**
     * Reads and parses the string report generated for the INFO Redis command in order to return any
     * specific required property.
     *
     * @param redisReport
     * @return Map
     */
    private Map<String, String> getRedisProperties(final String redisReport) {

        final Map<String, String> map = new LinkedHashMap<>();
        final String[] readLines = redisReport.split("\r\n");

        for (final String readLine : readLines) {

            final String[] lineValues = readLine.split(":", 2);

            // First check if it is a property or a header
            if (lineValues.length > 1) {

                map.put(lineValues[0], lineValues[1]);
            }
        }

        return map;
    }

    class CacheWiper implements Runnable {

        final String prefix;

        @Override
        public String toString() {
            return "CacheWiper prefix:" + prefix;
        }

        public CacheWiper(final String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void run() {

            RedisCache.this.client.scanKeys(this.prefix, keyBatchingSize,
                    keyCollections ->  removeKeys(keyCollections.toArray(new String[0])));
        }
    }

    class PrefixChecker implements Runnable {

        @Override
        public void run() {

            final String ourPrefix =  prefixKey.get();
            final String redisPrefix = loadPrefixFromRedis();
            // force a reload if needed
            if(!Objects.equal(ourPrefix, redisPrefix)) {
                prefixKey.set(PREFIX_UNSET);
            }
        }
    }
}
