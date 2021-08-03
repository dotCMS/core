package com.dotcms.cache.lettuce;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.repackage.com.google.common.base.Objects;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Redis Cache implementation
 */
public class LettuceCache extends CacheProvider {

    private static final Long ZERO = new Long(0);
    private static final String KEY_DATE_FORMAT = "yyyy-MM-dd_hh:mm:ss.S";

    private final String clusterId;
    private final RedisClient<String, Object> lettuce;
    private final static String PREFIX_UNSET = "PREFIX_UNSET";

    private final String LETTUCE_GROUP_KEY;
    private final String LETTUCE_PREFIX_KEY;

    private final long dirtyReadDelayMs   = Config.getLongProperty("redis.lettucecache.dirtyread.delay.ms", 10000L);
    private final long dirtyReadCacheSize = Config.getLongProperty("redis.lettucecache.dirtyread.size", 100000L);

    /**
     * The dirtyReadCache caches every write (put/remove) sent to Redis and will not allow a get for
     * that key until 10 seconds has passed. This allows us to do async writes with confidence.
     */
    private final Cache<String, Boolean> dirtyReadCache = Caffeine.newBuilder().maximumSize(dirtyReadCacheSize)
            .expireAfterWrite(dirtyReadDelayMs, TimeUnit.MILLISECONDS).build();

    private static final long serialVersionUID = -855583393078878276L;
    private final int keyBatchingSize = Config.getIntProperty("redis.server.key.batch.size", 1000);
    final static AtomicReference<String> prefixKey = new AtomicReference(PREFIX_UNSET);

    public LettuceCache(final RedisClient<String,Object> client, final String clusterId) {

        this.lettuce = client;
        this.clusterId = clusterId;
        this.LETTUCE_GROUP_KEY = clusterId + "LETTUCE_GROUP_KEY";
        this.LETTUCE_PREFIX_KEY = clusterId + "LETTUCE_PREFIX_KEY";
    }

    public LettuceCache() {
        this(new MasterReplicaLettuceClient<>(), APILocator.getShortyAPI().shortify(ClusterFactory.getClusterId()));
    }

    @Override
    public String getName() {
        return "Lettuce Provider";
    }

    @Override
    public String getKey() {
        return "lettuceProvider";
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

        try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {

            return (String) conn.sync().get(LETTUCE_PREFIX_KEY);
        } catch (Exception e) {

            Logger.debug(this.getClass(), ()-> "unable to get prefix:" + e.getMessage());
            return PREFIX_UNSET;
        }
    }

    String generateNewKey() {

        return clusterId + StringPool.UNDERLINE +
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
        try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {
            if (!conn.isOpen()) {
                return PREFIX_UNSET;
            }
            conn.sync().set(LETTUCE_PREFIX_KEY, newKey);
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

        final String newKey = this.generateNewKey();
        try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {
            if (!conn.isOpen()) {
                return PREFIX_UNSET;
            }
            return conn.sync().setnx(LETTUCE_PREFIX_KEY, newKey)?
                    newKey : this.loadPrefixFromRedis();
        }
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

        if (key == null || group == null) {
            return;
        }

        final String cacheKey = this.cacheKey(group, key);
        this.dirtyReadCache.put(cacheKey, true); // todo: this should not be handle by the cache chain?

        try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {

            if (conn.isOpen()) {

                conn.async().sadd(LETTUCE_GROUP_KEY, group);
                conn.async().set(cacheKey, content);
            }
        }

    }

    @Override
    public Object get(final String group, final String key) {

        if (key == null || group == null) {
            return null;
        }

        final String cacheKey = cacheKey(group, key);

        if (this.dirtyReadCache.get(cacheKey(group), k -> false)) {
            return null;
        }

        try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {

            if (conn.isOpen()) {
                return conn.sync().get(cacheKey);
            }
        }

        return null;
    }

    /**
     * removes cache keys async and resets the get timer that reenables get functions
     * 
     * @param keys
     */
    private void remove(final String... keys) {

        if (keys == null || keys.length == 0) {

            return;
        }

        for (final String key : keys) {

            dirtyReadCache.put(key, true);
        }

        try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {

            if (conn.isOpen()) {

                conn.async().unlink(keys);
            }
        }
    }



    @Override
    public void remove(final String group, final String key) {

        final String cacheKey = cacheKey(group, key);
        this.remove(cacheKey);
    }

    @Override
    public void remove(final String group) {

        if (UtilMethods.isEmpty(group)) {

            return;
        }

        this.dirtyReadCache.put(cacheKey(group), true);  // todo: why true

        final String prefix = cacheKey(group) + "*";
        // Getting all the keys for the given groups
        final CacheWiper wiper = new CacheWiper(prefix);

        DotConcurrentFactory.getInstance().getSingleSubmitter(CacheWiper.class.getSimpleName()).submit(wiper);
    }

    @Override
    public void removeAll() {

        final String prefix = cacheKey("*");
        cycleKey(); // todo: why call this?
        // Getting all the keys for the given groups
        final CacheWiper wiper = new CacheWiper(prefix);
        DotConcurrentFactory.getInstance().getSingleSubmitter(CacheWiper.class.getSimpleName()).submit(wiper);
    }

    @Override
    public Set<String> getKeys(final String group) {

        final String prefix = cacheKey(group) + "*";
        KeyScanCursor<String> scanCursor = null;
        final Set<String> keys = new HashSet<>();

        try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {

            if (!conn.isOpen()) {

                return Collections.emptySet();
            }

            do {

                scanCursor = scanCursor == null?
                        conn.sync().scan(ScanArgs.Builder.matches(prefix).limit(keyBatchingSize)):
                        conn.sync().scan(scanCursor, ScanArgs.Builder.matches(prefix).limit(keyBatchingSize));

                keys.addAll(scanCursor.getKeys());
            } while (!scanCursor.isFinished());
        }

        return keys;
    }


    @VisibleForTesting
    Set<String> getAllKeys() {

        final String prefix = cacheKey(null) + "*";
        KeyScanCursor<String> scanCursor = null;
        final Set<String> keys = new HashSet<>();
        try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {
            if (!conn.isOpen()) {

                return Collections.emptySet();
            }
            do {
                scanCursor = scanCursor == null?
                        conn.sync().scan(ScanArgs.Builder.matches(prefix).limit(keyBatchingSize)):
                        conn.sync().scan(scanCursor, ScanArgs.Builder.matches(prefix).limit(keyBatchingSize));

                keys.addAll(scanCursor.getKeys());
            } while (!scanCursor.isFinished());
        }

        return keys;
    }


    /**
     * returns the number of cache keys in any given group
     * 
     * @param group
     * @return
     */
    private long keyCount(final String group) {

        final String prefix = cacheKey(group) + "*";

        final String script = "return #redis.pcall('keys', '" + prefix + "')";

        Object keyCount = ZERO;
        try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {

            if (!conn.isOpen()) {
                return 0;
            }

            keyCount = conn.sync().eval(script, ScriptOutputType.INTEGER, "0");
        }

        return (Long) keyCount;
    }



    @Override
    public Set<String> getGroups() {

        try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {

            if (!conn.isOpen()) {

                return Collections.emptySet();
            }

            return conn.sync().smembers(LETTUCE_GROUP_KEY).stream()
                    .map(k -> k.toString()).collect(Collectors.toSet());
        }
    }

    @Override
    public CacheProviderStats getStats() {

        final CacheStats providerStats = new CacheStats();
        final CacheProviderStats cacheProviderStats = new CacheProviderStats(providerStats, getName());
        String memoryStats = null;

        try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {

            if (!conn.isOpen()) {

                return cacheProviderStats;
            }

            memoryStats = conn.sync().info();
        }

        // Read the total memory usage
        final Map<String, String> redis = getRedisProperties(memoryStats);

        for (Map.Entry<String, String> entry : redis.entrySet()) {
            CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, "redis: " + entry.getKey());
            stats.addStat(CacheStats.REGION_SIZE, entry.getValue());
            // ret.addStatRecord(stats);
        }

        NumberFormat nf = DecimalFormat.getInstance();
        // Getting the list of groups
        Set<String> currentGroups = getGroups();


        for (String group : currentGroups) {
            CacheStats stats = new CacheStats();
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
        // todo: should not do  redisClient.shutdown(Duration.ZERO, Duration.ZERO);
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

            KeyScanCursor<String> scanCursor = null;

            try (StatefulRedisConnection<String,Object> conn = lettuce.getConn()) {

                if (!conn.isOpen()) {

                    return;
                }

                do {
                    if (scanCursor == null) {
                        scanCursor = conn.sync().scan(ScanArgs.Builder.matches(prefix).limit(keyBatchingSize));
                    } else {
                        scanCursor = conn.sync().scan(scanCursor, ScanArgs.Builder.matches(prefix).limit(keyBatchingSize));
                    }

                    remove(scanCursor.getKeys().toArray(new String[0]));
                } while (!scanCursor.isFinished());
            }

        }
    }

    // todo: what is the goal of this?
    class PrefixChecker  implements Runnable {

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
