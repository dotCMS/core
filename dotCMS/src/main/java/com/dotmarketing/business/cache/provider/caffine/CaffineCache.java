package com.dotmarketing.business.cache.provider.caffine;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableSet;


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
        Cache<String, Object> defaultCache = getCache(DEFAULT_CACHE);
        NumberFormat nf = DecimalFormat.getInstance();
        DecimalFormat pf = new DecimalFormat("##.##%");
        for (String group : currentGroups) {
            CacheStats stats = new CacheStats();

            Cache<String, Object> foundCache = getCache(group);


            boolean isDefault = (Config.getIntProperty("cache." + group + ".size", -1) == -1);


            int configured = isDefault ? Config.getIntProperty("cache." + DEFAULT_CACHE + ".size")
                : (Config.getIntProperty("cache." + group + ".size", -1) != -1)
                  ? Config.getIntProperty("cache." + group + ".size")
                      : Config.getIntProperty("cache." + DEFAULT_CACHE + ".size");


            com.github.benmanes.caffeine.cache.stats.CacheStats cstats = foundCache.stats();
            stats.addStat(CacheStats.REGION, group);
            stats.addStat(CacheStats.REGION_DEFAULT, isDefault + "");
            stats.addStat(CacheStats.REGION_CONFIGURED_SIZE, nf.format(configured));
            stats.addStat(CacheStats.REGION_SIZE, nf.format(foundCache.estimatedSize()));
            stats.addStat(CacheStats.REGION_LOAD, nf.format(cstats.missCount()+cstats.hitCount()));
            stats.addStat(CacheStats.REGION_HITS, nf.format(cstats.hitCount()));
            stats.addStat(CacheStats.REGION_HIT_RATE, pf.format(cstats.hitRate()));
            stats.addStat(CacheStats.REGION_AVG_LOAD_TIME, nf.format(cstats.averageLoadPenalty()/1000000) + " ms");
            stats.addStat(CacheStats.REGION_EVICTIONS, nf.format(cstats.evictionCount()));
            

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

                        Logger.info(this.getClass(),
                                "***\t Building Cache : " + cacheName + ", size:" + size
                                        + ",Concurrency:"
                                        + Config.getIntProperty("cache.concurrencylevel", 32));
                        cache = Caffeine.newBuilder()
                                .maximumSize(size)
                                .recordStats()
                                //.softValues()
                                .build();


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
