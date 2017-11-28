package com.dotmarketing.business.cache.provider.ignite;

import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotcms.enterprise.cluster.ClusterFactory;

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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.cache.Cache;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;


public class IgniteCacheProvider extends CacheProvider {


    private long lastLog = System.currentTimeMillis();
    private static final long serialVersionUID = 1348649382678659786L;
    private boolean initialized = false;
    private static final String DEFAULT_CACHE = CacheProviderAPI.DEFAULT_CACHE;
    private final ConcurrentHashMap<String, IgniteCache<String, Object>> groups = new ConcurrentHashMap<>();
    private final String clusterId;
    private IgniteClient client;

    protected IgniteCacheProvider(String clusterId) {
        this.clusterId = clusterId;
    }

    public IgniteCacheProvider() {
        this(ClusterFactory.getClusterId().substring(0, 8));
    }

    @Override
    public String getName() {
        return "Ignite Cache Provider";
    }

    @Override
    public String getKey() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean isDistributed() {
        return true;
    }

    @Override
    public void init() {
        igniteClient();
    }

    @Override
    public boolean isInitialized() throws Exception {
        return initialized;
    }

    @Override
    public void put(String group, String key, Object content) {

        getCache(group).ifPresent(cache -> {
            try {
                cache.put(key, content);
            } catch (Exception e) {
                handleError(e);
            }
        });
    }

    @Override
    public Object get(final String group, final String key) {
        return getCache(group).map(cache -> {
            try {
                return cache.get(key);
            } catch (Exception e) {
                handleError(e);
                return null;
            }
        }).orElse(null);

    }

    @Override
    public void remove(String group) {
        getCache(group).ifPresent(cache -> {
            try {
                groups.remove(cache.getName());
                cache.destroy();
            } catch (Exception e) {
                handleError(e);
            }
        });
    }

    @Override
    public void remove(String group, String key) {
        getCache(group).ifPresent(cache -> {
            try {
                // Invalidates from Cache a key from a given group
                cache.clear(key);
            } catch (Exception e) {
                handleError(e);
            }
        });

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

        getCache(group).ifPresent(cache -> {
            Iterator<Cache.Entry<String, Object>> iter = cache.iterator();
            while (iter.hasNext()) {
                keys.add(iter.next().getKey());
            }
        });
        return keys;
    }

    @Override
    public CacheProviderStats getStats() {

        final CacheStats providerStats = new CacheStats();
        final CacheProviderStats ret = new CacheProviderStats(providerStats, getName());



        IgniteCache<String, Object> defaultCache = getCache(DEFAULT_CACHE).orElseThrow(IllegalStateException::new);



        final Set<String> currentGroups = new HashSet<>();
        currentGroups.addAll(getGroups());

        NumberFormat nf = DecimalFormat.getInstance();
        DecimalFormat pf = new DecimalFormat("##.##%");
        for (final String group : currentGroups) {


            getCache(group).ifPresent(foundCache -> {
                CacheStats stats = new CacheStats();

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
            });
        }

        return ret;
    }

    @Override
    public void shutdown() {
        synchronized (IgniteClient.class) {
            this.initialized = false;
            Logger.info(this.getClass(), "===== Shutting down [" + getName() + "].");
            IgniteClient old = this.client;
            this.client = null;
            this.groups.clear();
            old.stop();
        }
    }

    private Optional<IgniteCache<String, Object>> getCache(String cacheName) {

        if (cacheName == null) {
            throw new DotStateException("Null cache region passed in");
        }
        if (!this.initialized) {
            return Optional.empty();
        }

        cacheName = cacheName.toLowerCase();
        IgniteCache<String, Object> cache = groups.get(cacheName);

        // init cache if it does not exist
        if (cache == null) {
            synchronized (cacheName.intern()) {
                cache = groups.get(cacheName);
                if (cache == null) {

                    cache = buildCacheRegion(cacheName);

                }
            }
        }

        return Optional.ofNullable(cache);



    }


    private IgniteCache<String, Object> buildCacheRegion(String cacheName) {

        // Set replicas/backups automatically
        int backups = ClusterFactory.getNodeCount() - 1;
        backups = (backups < 0) ? 0 : (backups > 3) ? 3 : backups;
        backups = Config.getIntProperty("cache.ignite.backups", backups);


        // resolve cache sizing
        int size = (Config.getIntProperty("cache." + cacheName + ".size", -1) == -1)
                ? Config.getIntProperty("cache." + DEFAULT_CACHE + ".size", 100)
                : Config.getIntProperty("cache." + cacheName + ".size", 100);

        CacheConfiguration<String, Object> config = new CacheConfiguration<String, Object>().setStatisticsEnabled(true)
                .setName(cacheName)
                .setAtomicityMode(CacheAtomicityMode.ATOMIC)
                .setRebalanceThrottle(100)
                .setCopyOnRead(false)
                .setCacheMode(CacheMode.PARTITIONED)
                .setWriteSynchronizationMode(CacheWriteSynchronizationMode.PRIMARY_SYNC)
                .setBackups(backups);


        LruEvictionPolicy<String, Object> lru = new LruEvictionPolicy<String, Object>().setMaxSize(size);

        NearCacheConfiguration<String, Object> near = new NearCacheConfiguration<String, Object>().setNearEvictionPolicy(lru);
        try {
            IgniteCache<String, Object> cache = igniteClient().ignite().getOrCreateCache(config, near);
            Logger.info(this.getClass(), "***\t Building Cache : " + config);
            groups.put(cacheName, cache);
            return cache;
        } catch (javax.cache.CacheException jcc) {
            Logger.warn(this.getClass(), "failed building cache:" + cacheName + " " + jcc.getMessage());
        }
        return null;

    }

    private IgniteClient igniteClient() {
        if (client == null) {
            synchronized (IgniteClient.class) {
                if (client == null) {
                    client = new IgniteClient(clusterId);
                    this.initialized = true;
                }
            }
        }
        return client;
    }



    // limit error message to every 5 seconds;
    private final int limitErrorLogMillis = Config.getIntProperty("cache.ignite.limit.one.error.log.per.milliseconds", 10000);

    private void handleError(final Exception ex) {
        final Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
        if (cause instanceof org.apache.ignite.internal.processors.cache.CacheStoppedException) {
            return;
        }
        // debug all errors
        Logger.debug(this.getClass(), ex.getMessage() + " on " + ex.getMessage());
        if (lastLog + limitErrorLogMillis < System.currentTimeMillis()) {
            lastLog = System.currentTimeMillis();
            Logger.warn(this.getClass(), ex.getMessage(), ex);
        }
    }

}
