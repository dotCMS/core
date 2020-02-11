package com.dotcms.cache.lettuce;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.Tuple2;


public class LettuceCache extends CacheProvider {
    private final LettuceClient lettuce;

    public LettuceCache(LettuceClient client) {
        lettuce = client;
    }

    public LettuceCache() {
        this(LettuceClient.getInstance());
    }

    private static final long serialVersionUID = -855583393078878276L;
    private int keyBatching = Config.getIntProperty("redis.server.key.batch.size", 5000);
    private boolean isInitialized = false;

    private final static String CACHE_GROUP_KEY="CACHE_GROUP_KEY";
    // Global Map of contents that could not be added to this cache
    private static Cache<String, String> cannotCacheCache =
                    Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(10, TimeUnit.MINUTES).build();

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
     * returns a cache key
     * 
     * @param group
     * @param key
     * @return
     */
    private Tuple2<String, String> cacheKey(String group, String key) {
        return new Tuple2<String, String>((group != null) ? group.toLowerCase() : null, (key != null) ? key.toLowerCase() : null);
    }

    @Override
    public void init() {
        Logger.info(this.getClass(), "*** Initializing [" + getName() + "].");
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            Logger.info(this.getClass(), conn.sync().info());
        }
        Logger.info(this.getClass(), "*** Initialized Cache Provider [" + getName() + "].");
        isInitialized = true;
    }

    @Override
    public boolean isInitialized() throws Exception {
        return isInitialized;
    }

    @Override
    public void put(String group, String key, Object content) {

        if (key == null || group == null) {
            return;
        }
        final Tuple2<String, String> cacheKey = cacheKey(group, key);

        if (cannotCacheCache.getIfPresent(cacheKey.toString()) != null) {
            Logger.debug(this, "Returning because object is in cannot cache cache - Redis: group [" + group + "] - key [" + key
                            + "].");
            return;
        }

        // Check if we must exclude this record from this cache
        if (exclude(cacheKey)) {
            return;
        }
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.async().sadd(CACHE_GROUP_KEY, group);

            conn.async().set(cacheKey.toString(), content);
        }
        
    }

    @Override
    public Object get(String group, String key) {

        if (key == null || group == null) {
            return null;
        }
        final Tuple2<String, String> cacheKey = cacheKey(group, key);

        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            return conn.sync().get(cacheKey.toString());
        }


    }

    @Override
    public void remove(final String group, final String key) {

        final Tuple2<String, String> cacheKey = cacheKey(group, key);

        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().unlink(cacheKey.toString());
        }
    }

    @Override
    public void remove(String group) {

        if (UtilMethods.isEmpty(group)) {
            return;
        }


        // Getting all the keys for the given groups
        getKeys(group).forEach(k -> remove(k));



    }

    @Override
    public void removeAll() {
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().flushall();
        }
    }

    @Override
    public Set<String> getKeys(String group) {
        group = group != null ? group.toLowerCase() : "";
        KeyScanCursor<String> scanCursor = null;
        RedisFuture<KeyScanCursor<String>> future = null;
        Set<String> keys = new HashSet<String>();
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            do {
                if (scanCursor == null) {
                    future = conn.async().scan(ScanArgs.Builder.matches("(" + group + ",").limit(keyBatching));
                } else {
                    future = conn.async().scan(scanCursor, ScanArgs.Builder.matches(group).limit(keyBatching));
                }

                try {
                    scanCursor = future.get();
                } catch (Exception e) {
                    return null;
                }
                keys.addAll(scanCursor.getKeys());

            } while (!scanCursor.isFinished());
        }

        return keys;


    }

    @Override
    public Set<String> getGroups() {
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            return conn.sync().smembers(CACHE_GROUP_KEY).stream().map(k->k.toString()).collect(Collectors.toSet());
        }
    }

    @Override
    public CacheProviderStats getStats() {
        CacheStats providerStats = new CacheStats();
        CacheProviderStats ret = new CacheProviderStats(providerStats, getName());
        String memoryStats = null;
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            memoryStats = conn.sync().info("memory");
        }

        // Read the total memory usage
        int memoryUsage = -1;
        String memoryUsageString = getRedisProperty(memoryStats, "used_memory");
        if (memoryUsageString != null) {
            memoryUsage = Integer.valueOf(memoryUsageString);
        }
        NumberFormat nf = DecimalFormat.getInstance();
        // Getting the list of groups
        Set<String> currentGroups = getGroups();

        for (String group : currentGroups) {
            CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, group);
            stats.addStat(CacheStats.REGION_SIZE, nf.format(getKeys(group).size()));
            stats.addStat(CacheStats.REGION_CONFIGURED_SIZE, nf.format(memoryUsage));
            /*
             * Show the complete memory usage just one time, the cache stats page needs improvements
             * (html/portlet/ext/cmsmaintenance/cachestats_guava.jsp), that page was not created in caches that
             * does not rely on groups
             */
            memoryUsage = 0;
            ret.addStatRecord(stats);
        }

        return ret;
    }

    @Override
    public void shutdown() {

        Logger.info(this.getClass(), "*** Destroying [" + getName() + "] pool.");


        isInitialized = false;
    }

    /**
     * Reads and parses the string report generated for the INFO Redis command in order to return any
     * specific required property.
     *
     * @param redisReport
     * @param property
     * @return
     */
    private String getRedisProperty(String redisReport, String property) {

        String value = null;

        String[] readLines = redisReport.split("\r\n");
        for (String readLine : readLines) {

            String[] lineValues = readLine.split(":");

            // First check if it is a property or a header
            if (lineValues.length > 1) {

                String currentProperty = lineValues[0];
                String currentValue = lineValues[1];

                if (currentProperty.equals(property)) {
                    // Found it
                    value = currentValue;
                    break;
                }
            }
        }

        return value;
    }

    /**
     * Resets the Map of cache records that could be added to this cache
     */
    private void resetCannotCacheCache() {
        cannotCacheCache = Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(10, TimeUnit.MINUTES)
                        // .softValues()
                        .build();
    }


    /**
     * Method that verifies if must exclude content that can not or must not be added to this Redis
     * cache based on the given cache group and key
     *
     * @param group
     * @param key
     * @return
     */
    private boolean exclude(Tuple2<String, String> key) {

        boolean exclude = false;

        if (key._1.equals(ONLY_MEMORY_GROUP)) {
            exclude = true;

            cannotCacheCache.put(key.toString(), key.toString());
        }

        return exclude;
    }

}
