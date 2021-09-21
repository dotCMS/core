package com.dotmarketing.business.cache.provider.caffine;

import com.dotcms.cache.Expirable;
import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheSizingUtil;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Try;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * In-Memory Cache implementation using https://github.com/ben-manes/caffeine
 *
 * Supports key-specific time invalidations by providing an {@link Expirable} object
 * in the {@link #put(String, String, Object)} method with the desired TTL.
 *
 * A group-wide invalidation time can also be set by config properties.
 *
 * i.e., for the "graphqlquerycache" group
 *
 * cache.graphqlquerycache.chain=com.dotmarketing.business.cache.provider.caffine.CaffineCache
 * cache.graphqlquerycache.seconds=15
 *
 */
public class CaffineCache extends CacheProvider {

    private static final long serialVersionUID = 1348649382678659786L;

    private Boolean isInitialized = false;

    static final String DEFAULT_CACHE = CacheProviderAPI.DEFAULT_CACHE;

    private final ConcurrentHashMap<String, Cache<String, Object>> groups =
            new ConcurrentHashMap<>();
    private Set<String> availableCaches;


    @Override
    public String getName() {
        return "Caffine Memory Cache";
    }

    @Override
    public String getKey() {
        return "LocalCaffineMem";
    }

    @Override
    public boolean isDistributed() {
        return false;
    }

    @Override
    public void init() {
        HashSet<String> _availableCaches = new HashSet<>();
        _availableCaches.add(DEFAULT_CACHE);

        Iterator<String> it = Config.getKeys();
        while (it.hasNext()) {

            String key = it.next();
            if (key == null) {
                continue;
            }

            if (key.startsWith("cache.")) {

                String cacheName = key.split("\\.")[1];
                if (key.endsWith(".size")) {
                    int inMemory = Config.getIntProperty(key, 0);
                    _availableCaches.add(cacheName.toLowerCase());
                    Logger.info(this.getClass(),
                            "***\t Cache Config Memory : " + cacheName + ": " + inMemory);
                }

            }
        }
        this.availableCaches = ImmutableSet.copyOf(_availableCaches);
        isInitialized = true;
    }

    @Override
    public boolean isInitialized() throws Exception {
        return isInitialized;
    }

    @Override
    public void put(String group, String key, Object content) {
        // Get the cache for the given group
        Cache<String, Object> cache = getCache(group);
        // Add the given content to the group and for a given key
        cache.put(key, content);
    }

    @Override
    public Object get(String group, String key) {
        // Get the cache for the given group
        Cache<String, Object> cache = getCache(group);
        // Get the content from the group and for a given key
        return cache.getIfPresent(key);
    }

    @Override
    public void remove(String group) {

        // Get the cache for the given group
        Cache<String, Object> cache = getCache(group);

        // Invalidates the Cache for the given group
        cache.invalidateAll();

        // Remove this group from the global list of cache groups
        groups.remove(group);
    }

    @Override
    public void remove(String group, String key) {

        // Get the cache for the given group
        Cache<String, Object> cache = getCache(group);

        // Invalidates from Cache a key from a given group
        cache.invalidate(key);
    }

    @Override
    public void removeAll() {

        Set<String> currentGroups = new HashSet<>();
        currentGroups.addAll(getGroups());

        for (String group : currentGroups) {
            remove(group);
        }

        groups.clear();
    }

    @Override
    public Set<String> getGroups() {
        return groups.keySet();
    }

    @Override
    public Set<String> getKeys(String group) {

        Set<String> keys = new HashSet<>();

        Cache<String, Object> cache = getCache(group);
        Map<String, Object> m = cache.asMap();

        if (m != null) {
            keys = m.keySet();
        }

        return keys;
    }

    @Override
    public CacheProviderStats getStats() {

        CacheStats providerStats = new CacheStats();
        CacheProviderStats ret = new CacheProviderStats(providerStats,getName());

        Set<String> currentGroups = new HashSet<>();
        currentGroups.addAll(getGroups());
        NumberFormat nf = DecimalFormat.getInstance();
        DecimalFormat pf = new DecimalFormat("##.##%");

        CacheSizingUtil sizer = new CacheSizingUtil();



        for (String group : currentGroups) {
            final CacheStats stats = new CacheStats();

            final Cache<String, Object> foundCache = getCache(group);


            final boolean isDefault = (Config.getIntProperty("cache." + group + ".size", -1) == -1);


            final int size = isDefault ? Config.getIntProperty("cache." + DEFAULT_CACHE + ".size")
                    : (Config.getIntProperty("cache." + group + ".size", -1) != -1)
                            ? Config.getIntProperty("cache." + group + ".size")
                            : Config.getIntProperty("cache." + DEFAULT_CACHE + ".size");

            final int seconds = isDefault ? Config.getIntProperty("cache." + DEFAULT_CACHE + ".seconds", 100)
                    : (Config.getIntProperty("cache." + group + ".seconds", -1) != -1)
                            ? Config.getIntProperty("cache." + group + ".seconds")
                            : Config.getIntProperty("cache." + DEFAULT_CACHE + ".seconds", 100);

            com.github.benmanes.caffeine.cache.stats.CacheStats cstats = foundCache.stats();
            stats.addStat(CacheStats.REGION, group);
            stats.addStat(CacheStats.REGION_DEFAULT, isDefault + "");
            stats.addStat(CacheStats.REGION_CONFIGURED_SIZE, "size:" + nf.format(size) + " / " + seconds + "s");
            stats.addStat(CacheStats.REGION_SIZE, nf.format(foundCache.estimatedSize()));
            stats.addStat(CacheStats.REGION_LOAD, nf.format(cstats.missCount()+cstats.hitCount()));
            stats.addStat(CacheStats.REGION_HITS, nf.format(cstats.hitCount()));
            stats.addStat(CacheStats.REGION_HIT_RATE, pf.format(cstats.hitRate()));
            stats.addStat(CacheStats.REGION_AVG_LOAD_TIME, nf.format(cstats.averageLoadPenalty()/1000000) + " ms");
            stats.addStat(CacheStats.REGION_EVICTIONS, nf.format(cstats.evictionCount()));

            long averageObjectSize = Try.of(()-> sizer.averageSize(foundCache.asMap())).getOrElse(-1L);
            long totalObjectSize = averageObjectSize * foundCache.estimatedSize();

            stats.addStat(CacheStats.REGION_MEM_PER_OBJECT, "<div class='hideSizer'>" + String.format("%010d", averageObjectSize) + "</div>" + UtilMethods.prettyByteify(averageObjectSize));
            stats.addStat(CacheStats.REGION_MEM_TOTAL_PRETTY, "<div class='hideSizer'>" + String.format("%010d", totalObjectSize) + "</div>" + UtilMethods.prettyByteify(totalObjectSize));
            ret.addStatRecord(stats);
        }

        return ret;
    }

    @Override
    public void shutdown() {
        Logger.info(this.getClass(), "===== Calling shutdown [" + getName() + "].");
        isInitialized = false;
    }

    private Cache<String, Object> getCache(String cacheName) {

        if (cacheName == null) {
            throw new DotStateException("Null cache region passed in");
        }

        cacheName = cacheName.toLowerCase();
        Cache<String, Object> cache = groups.get(cacheName);

        // init cache if it does not exist
        if (cache == null) {
            synchronized (cacheName.intern()) {
                cache = groups.get(cacheName);
                if (cache == null) {

                    boolean separateCache = (Config.getBooleanProperty(
                            "cache.separate.caches.for.non.defined.regions", true)
                            || availableCaches.contains(cacheName)
                            || DEFAULT_CACHE.equals(cacheName));

                    if (separateCache) {
                        int size = Config.getIntProperty("cache." + cacheName + ".size", -1);

                        if (size == -1) {
                            size = Config.getIntProperty("cache." + DEFAULT_CACHE + ".size", 100);
                        }

                        final int defaultTTL = Config.getIntProperty("cache." + cacheName + ".seconds", -1);


                        Logger.info(this.getClass(),
                                "***\t Building Cache : " + cacheName + ", size:" + size
                                        + ",Concurrency:"
                                        + Config.getIntProperty("cache.concurrencylevel", 32));

                        cache = Caffeine.newBuilder()
                                .maximumSize(size)
                                .recordStats()
                                .expireAfter(new Expiry<String, Object>() {
                                    public long expireAfterCreate(String key, Object value, long currentTime) {
                                        long ttlInSeconds;

                                        if(value instanceof Expirable
                                                && ((Expirable) value).getTtl() > 0) {
                                            ttlInSeconds = ((Expirable) value).getTtl();
                                        } else if (defaultTTL > 0) {
                                            ttlInSeconds = defaultTTL;
                                        } else {
                                            ttlInSeconds = Long.MAX_VALUE;
                                        }

                                        return TimeUnit.SECONDS.toNanos(ttlInSeconds);
                                    }
                                    public long expireAfterUpdate(String key, Object value,
                                            long currentTime, long currentDuration) {
                                        return currentDuration;
                                    }
                                    public long expireAfterRead(String key, Object value,
                                            long currentTime, long currentDuration) {
                                        return currentDuration;
                                    }
                                })
                                .build(key -> null);

                        groups.put(cacheName, cache);

                    } else {
                        Logger.info(this.getClass(),
                                "***\t No Cache for   : " + cacheName + ", using " + DEFAULT_CACHE);
                        cache = getCache(DEFAULT_CACHE);
                        groups.put(cacheName, cache);
                    }
                }
            }
        }
        return cache;
    }
}
