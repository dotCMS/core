package com.dotmarketing.business.cache.provider.ignite;

import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotcms.enterprise.cluster.ClusterFactory;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.cache.Cache;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;

import com.google.common.collect.ImmutableSet;


public class IgniteCacheProvider extends CacheProvider {




    private static final long serialVersionUID = 1348649382678659786L;
    private Boolean isInitialized = false;
    static final String DEFAULT_CACHE = CacheProviderAPI.DEFAULT_CACHE;
    private final ConcurrentHashMap<String, IgniteCache<String, Object>> groups = new ConcurrentHashMap<>();

    private Set<String> availableCaches;


    @Override
    public String getName() {
        return "Ignite Grid Cache";
    }

    @Override
    public String getKey() {
        return "IgniteGridCache";
    }

    @Override
    public boolean isDistributed() {
        return true;
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
                    Logger.info(this.getClass(), "***\t Cache Config Memory : " + cacheName + ": " + inMemory);
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
        IgniteCache<String, Object> cache = getCache(group);

        // Add the given content to the group and for a given key
        cache.put(key, content);
    }

    @Override
    public Object get(String group, String key) {

        // Get the cache for the given group
        IgniteCache<String, Object> cache = getCache(group);



        // Get the content from the group and for a given key
        return cache.get(key);


    }

    @Override
    public void remove(String group) {

        // Get the cache for the given group
        IgniteCache<String, Object> cache = getCache(group);
        groups.remove(group);
        cache.destroy();



    }

    @Override
    public void remove(String group, String key) {

        // Get the cache for the given group
        IgniteCache<String, Object> cache = getCache(group);

        // Invalidates from Cache a key from a given group
        cache.clearAsync(key);
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

        IgniteCache<String, Object> cache = getCache(group);
        Iterator<Cache.Entry<String, Object>> iter = cache.iterator();
        while (iter.hasNext()) {
            keys.add(iter.next().getKey());
        }
        return keys;
    }

    @Override
    public CacheProviderStats getStats() {

        CacheStats providerStats = new CacheStats();
        CacheProviderStats ret = new CacheProviderStats(providerStats, getName());

        Set<String> currentGroups = new HashSet<>();
        currentGroups.addAll(getGroups());
        IgniteCache<String, Object> defaultCache = getCache(DEFAULT_CACHE);
        NumberFormat nf = DecimalFormat.getInstance();
        DecimalFormat pf = new DecimalFormat("##.##%");
        for (String group : currentGroups) {
            CacheStats stats = new CacheStats();

            IgniteCache<String, Object> foundCache = getCache(group);


            boolean isDefault = (Config.getIntProperty("cache." + group + ".size", -1) == -1);


            int configured = isDefault ? Config.getIntProperty("cache." + DEFAULT_CACHE + ".size")
                    : (Config.getIntProperty("cache." + group + ".size", -1) != -1)
                            ? Config.getIntProperty("cache." + group + ".size")
                            : Config.getIntProperty("cache." + DEFAULT_CACHE + ".size");

            CacheMetrics metrics = foundCache.localMetrics();
            stats.addStat(CacheStats.REGION, group);
            stats.addStat("local", "local");
            stats.addStat(CacheStats.REGION_DEFAULT, isDefault + "");
            stats.addStat(CacheStats.REGION_CONFIGURED_SIZE, nf.format(configured));
            stats.addStat(CacheStats.REGION_SIZE, nf.format(foundCache.size(CachePeekMode.ALL)));
            stats.addStat(CacheStats.REGION_LOAD, nf.format(metrics.getCacheMisses() + metrics.getCacheHits()));
            stats.addStat(CacheStats.REGION_HITS, nf.format(metrics.getCacheHits()));
            stats.addStat(CacheStats.REGION_HIT_RATE, nf.format(Math.round(metrics.getCacheHitPercentage())) + "%");
            stats.addStat(CacheStats.REGION_AVG_LOAD_TIME, nf.format(metrics.getAverageGetTime() / 1000) + " ms");
            stats.addStat(CacheStats.REGION_EVICTIONS, nf.format(metrics.getCacheEvictions()));

            ret.addStatRecord(stats);

            stats = new CacheStats();
            metrics = foundCache.metrics();
            stats.addStat(CacheStats.REGION, group);
            stats.addStat("local", "remote");
            stats.addStat(CacheStats.REGION_DEFAULT, isDefault + "");
            stats.addStat(CacheStats.REGION_CONFIGURED_SIZE, nf.format(configured));
            stats.addStat(CacheStats.REGION_SIZE, foundCache.localSizeLong(CachePeekMode.ALL));
            stats.addStat(CacheStats.REGION_LOAD, nf.format(metrics.getCacheMisses() + metrics.getCacheHits()));
            stats.addStat(CacheStats.REGION_HITS, nf.format(metrics.getCacheHits()));
            stats.addStat(CacheStats.REGION_HIT_RATE, nf.format(Math.round(metrics.getCacheHitPercentage())) + "%");
            stats.addStat(CacheStats.REGION_AVG_LOAD_TIME, nf.format(metrics.getAverageGetTime() / 1000) + " ms");
            stats.addStat(CacheStats.REGION_EVICTIONS, nf.format(metrics.getCacheEvictions()));
            ret.addStatRecord(stats);
        }

        return ret;
    }

    @Override
    public void shutdown() {
        Logger.info(this.getClass(), "===== Calling shutdown [" + getName() + "].");
        isInitialized = false;
    }

    private IgniteCache<String, Object> getCache(String cacheName) {

        if (cacheName == null) {
            throw new DotStateException("Null cache region passed in");
        }

        cacheName = cacheName.toLowerCase();
        IgniteCache<String, Object> cache = groups.get(cacheName);

        
        /*
         * Set backups automatically
         */
        int backups = ClusterFactory.getNodeCount()-1;        
        backups = (backups<0)  ? 0 : (backups > 3)  ? 3 : backups;
        
        // init cache if it does not exist
        if (cache == null) {
            synchronized (cacheName.intern()) {
                cache = groups.get(cacheName);
                if (cache == null) {

                    int size = Config.getIntProperty("cache." + cacheName + ".size", -1);

                    if (size == -1) {
                        size = Config.getIntProperty("cache." + DEFAULT_CACHE + ".size", 100);
                    }
                    CacheConfiguration<String, Object> config = new CacheConfiguration<String, Object>()
                            .setStatisticsEnabled(true)
                            .setName(cacheName)
                            .setAtomicityMode(CacheAtomicityMode.ATOMIC)
                            .setRebalanceThrottle(100)
                            .setOnheapCacheEnabled(true)
                            .setCopyOnRead(false)
                            .setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_ASYNC)
                            .setBackups(backups);
                    
                    LruEvictionPolicy<String, Object> lru = new LruEvictionPolicy<String, Object>().setMaxSize(size);
                    
                    NearCacheConfiguration<String, Object> near = new NearCacheConfiguration<String, Object>().setNearEvictionPolicy(lru);
               

                    cache = IgniteClient.getInstance().ignite().getOrCreateCache(config, near);
                    Logger.info(this.getClass(), "***\t Building Cache : " + config);


                    groups.put(cacheName, cache);


                }
            }
        }

        return cache;
    }

}
