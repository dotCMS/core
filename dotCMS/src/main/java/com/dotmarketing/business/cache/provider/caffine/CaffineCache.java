package com.dotmarketing.business.cache.provider.caffine;


import com.dotcms.cache.DynamicTTLCache;
import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * In-Memory Cache implementation using <a href="https://github.com/ben-manes/caffeine">...</a>
 * <p>
 * Supports key-specific time invalidations by using the method
 * {@link #put(String, String, Object, long)} method with the desired TTL.
 * <p>
 * A group-wide invalidation time can also be set by config properties.
 * <p>
 * i.e., for the "graphqlquerycache" group
 * <p>
 * cache.graphqlquerycache.chain=com.dotmarketing.business.cache.provider.caffine.CaffineCache
 * cache.graphqlquerycache.size=10000
 * cache.graphqlquerycache.seconds=15
 *
 * An individual object's TTL will override the group's TTL.
 *
 *
 */
public class CaffineCache extends CacheProvider {

    private static final String CACHE_PREFIX = "cache.";
    private static final String SIZE_POSTFIX = ".size";
    private static final String SECONDS_POSTFIX = ".seconds";
    private static final String DEFAULT_CACHE = CacheProviderAPI.DEFAULT_CACHE;
    private static final long serialVersionUID = 1L;

    // we use a cache for groups because concurrentHashMap has a recusion problem in its computeIfAbsent method
    private static final Cache<String, DynamicTTLCache<String, Object>> groups = Caffeine.newBuilder().maximumSize(10000).build();
    private static final AtomicBoolean isInitialized = new AtomicBoolean(false);

    @Override
    public String getName() {
        return "Caffeine Cache Provider";
    }

    @Override
    public String getKey() {
        return "CaffineCache";
    }

    @Override
    public boolean isDistributed() {
        return false;
    }

    @Override
    public void init() {
        // Prevent reinitialization during shutdown
        if (com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted()) {
            Logger.info(this.getClass(), "Shutdown in progress - skipping cache initialization");
            return;
        }
        
        if(!isInitialized.getAndSet(true)){
            groups.invalidateAll();
            Logger.info(this.getClass(), "===== Initializing [" + getName() + "].");
        }
    }

    @Override
    public boolean isInitialized() throws Exception {
        return isInitialized.get();
    }

    @Override
    public void put(String group, String key, Object content, long ttlMillis) {
        if ( key == null || content == null) {
            return;
        }
        DynamicTTLCache<String, Object> cache = getCache(group);
        cache.put(key, content, ttlMillis);

    }

    @Override
    public void put(String group, String key, Object content) {
        put(group, key, content, Long.MAX_VALUE);

    }

    @Override
    public Object get(String group, String key) {
        return getCache(group).getIfPresent(key);


    }

    @Override
    public void remove(String group, String key) {

        // Invalidates from Cache a key from a given group
        getCache(group).invalidate(key);
    }

    @Override
    public void remove(String group) {
        Logger.debug(this.getClass(), "===== Calling remove for [" + getName()
                + "] - " + group);
        if (group == null) {
            return;
        }
        groups.invalidate(group.toLowerCase());
    }

    @Override
    public void removeAll() {
        groups.invalidateAll();
    }

    @Override
    public Set<String> getKeys(String group) {
        return getCache(group).asMap().keySet();
    }

    @Override
    public Set<String> getGroups() {
        return groups.asMap().keySet();
    }

    private long getTTLMillis(String group) {
        if("system_cache".equals(group)){
            return UtilMethods.isSet(System.getenv("DOT_CACHE_SYSTEM_GROUP_SECONDS"))
                    ? Long.parseLong(System.getenv("DOT_CACHE_SYSTEM_GROUP_SECONDS"))
                    : Long.MAX_VALUE;
        }

        long seconds = Config.getLongProperty(CACHE_PREFIX + group + SECONDS_POSTFIX,
                Config.getLongProperty(CACHE_PREFIX + DEFAULT_CACHE + SECONDS_POSTFIX, -1));

        return seconds < 0 ? Long.MAX_VALUE : seconds * 1000;
    }

    private int getMaxSize(String group) {
        if("system_cache".equals(group)){
            return UtilMethods.isSet(System.getenv("DOT_CACHE_SYSTEM_GROUP_SIZE"))
                    ? Integer.parseInt(System.getenv("DOT_CACHE_SYSTEM_GROUP_SIZE"))
                    : 5000;
        }

        return Config.getIntProperty(CACHE_PREFIX + group + SIZE_POSTFIX,
                Config.getIntProperty(CACHE_PREFIX + DEFAULT_CACHE + SIZE_POSTFIX, 1000));

    }


    @Override
    public CacheProviderStats getStats() {

        CacheStats providerStats = new CacheStats();
        CacheProviderStats ret = new CacheProviderStats(providerStats, getName());

        Set<String> currentGroups = new TreeSet<>(getGroups());

        NumberFormat nf = DecimalFormat.getInstance();
        DecimalFormat pf = new DecimalFormat("##.##%");
        for (String group : currentGroups) {
            CacheStats stats = new CacheStats();

            DynamicTTLCache<String, Object> foundCache = getCache(group);

            long size = getMaxSize(group);

            long millis = foundCache.defaultTTLInMillis;

            String duration = millis == Long.MAX_VALUE ? "" : " | ttl:" + nf.format(millis / 1000) + "s";

            com.github.benmanes.caffeine.cache.stats.CacheStats cstats = foundCache.stats();
            stats.addStat(CacheStats.REGION, group)
                .addStat(CacheStats.REGION_DEFAULT, "false")
                .addStat(CacheStats.REGION_CONFIGURED_SIZE, "size:" + nf.format(size)  + duration )
                .addStat(CacheStats.REGION_SIZE, nf.format(foundCache.estimatedSize()))
                .addStat(CacheStats.REGION_LOAD, nf.format(cstats.missCount() + cstats.hitCount()))
                .addStat(CacheStats.REGION_HITS, nf.format(cstats.hitCount()))
                .addStat(CacheStats.REGION_HIT_RATE, pf.format(cstats.hitRate()))
                .addStat(CacheStats.REGION_AVG_LOAD_TIME, nf.format(cstats.averageLoadPenalty() / 1000000) + " ms")
                .addStat(CacheStats.REGION_EVICTIONS, nf.format(cstats.evictionCount()));
            ret.addStatRecord(stats);
        }

        if (currentGroups.isEmpty()) {
            CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, "n/a");
            stats.addStat(CacheStats.REGION_SIZE, 0);
            ret.addStatRecord(stats);
        }

        return ret;
    }

    @Override
    public void shutdown() {
        Logger.info(this.getClass(), "===== Calling shutdown [" + getName() + "].");
        isInitialized.set(false);
    }


    private DynamicTTLCache<String, Object> getCache(String cacheName) {
        if (cacheName == null) {
            throw new DotStateException("Null cache region passed in");
        }

        // Prevent cache reinitialization during shutdown
        if (com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted()) {
            Logger.debug(this.getClass(), "Shutdown in progress - returning null cache for: " + cacheName);
            return new DynamicTTLCache<>(1, 1000); // Return minimal cache to prevent NPE
        }

        DynamicTTLCache<String, Object> cache = groups.getIfPresent(cacheName);
        if(cache != null){
            return cache;
        }
        final int maxSize = getMaxSize(cacheName);
        final long ttlSeconds = getTTLMillis(cacheName);
        synchronized (groups) {
            return groups.get(cacheName, k ->

                    new DynamicTTLCache<>(maxSize, ttlSeconds)
            );
        }



    }

}
