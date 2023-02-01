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
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Try;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * In-Memory Cache implementation using <a href="https://github.com/ben-manes/caffeine">...</a>
 * <p>
 * Supports key-specific time invalidations by providing an {@link Expirable} object
 * in the {@link #put(String, String, Object)} method with the desired TTL.
 * <p>
 * A group-wide invalidation time can also be set by config properties.
 * <p>
 * i.e., for the "graphqlquerycache" group
 * <p>
 * cache.graphqlquerycache.chain=com.dotmarketing.business.cache.provider.caffine.CaffineCache
 * cache.graphqlquerycache.seconds=15
 *
 */
public class CaffineCache extends CacheProvider {

    private static final long serialVersionUID = 1348649382678659786L;
    public static final String SECONDS = ".seconds";
    public static final String SIZE = "size";
    public static final String CACHE = "cache.";

    private AtomicBoolean isInitialized=new AtomicBoolean(false);
    private AtomicBoolean initializing=new AtomicBoolean(false);

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
        if(!isInitialized.get() && initializing.compareAndSet(false, true)) {

            HashSet<String> tempAvailableCaches = new HashSet<>();
            tempAvailableCaches.add(DEFAULT_CACHE);

            Iterator<String> it = Config.getKeys();
            while (it.hasNext()) {

                String key = it.next();
                if (key == null) {
                    continue;
                }

                if (key.startsWith(CACHE)) {

                    String cacheName = key.split("\\.")[1];
                    if (key.endsWith("." + SIZE)) {
                        int inMemory = Config.getIntProperty(key, 0);
                        tempAvailableCaches.add(cacheName.toLowerCase());
                        Logger.debug(this.getClass(),
                                "***\t Cache Config Memory : " + cacheName + ": " + inMemory);
                    }

                }
            }
            this.availableCaches = ImmutableSet.copyOf(tempAvailableCaches);
            isInitialized.set(true);
            initializing.set(false);
        }
    }

    @Override
    public boolean isInitialized() throws Exception {
        return isInitialized.get();
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

       groups.computeIfPresent(group, (k, v) -> {
           v.invalidateAll();
           return null;});
    }

    @Override
    public void remove(String group, String key) {
        getCache(group).invalidate(key);
    }

    @Override
    public void removeAll() {
        groups.forEach((k, v) -> v.invalidateAll());
    }

    @Override
    public Set<String> getGroups() {
        return Set.copyOf(groups.keySet());
    }

    @Override
    public Set<String> getKeys(String group) {
        return Set.copyOf(getCache(group).asMap().keySet());
    }

    @Override
    public CacheProviderStats getStats() {

        CacheStats providerStats = new CacheStats();
        final CacheProviderStats ret = new CacheProviderStats(providerStats,getName());

        final NumberFormat nf = NumberFormat.getInstance();
        final DecimalFormat pf = new DecimalFormat("##.##%");

        final CacheSizingUtil sizer = new CacheSizingUtil();

        groups.forEach(1, (group, foundCache) -> processCacheStats(ret, nf, pf, sizer, group, foundCache)
        );

        return ret;
    }

    private static void processCacheStats(CacheProviderStats ret, NumberFormat nf, DecimalFormat pf,
            CacheSizingUtil sizer, String group, Cache<String, Object> foundCache) {
        final CacheStats stats = new CacheStats();
        boolean isDefault=false;
        int size=Config.getIntProperty(CACHE + group + "." + SIZE, -1);
        if (size==-1)
        {
            isDefault=true;
            size = Config.getIntProperty(CACHE + DEFAULT_CACHE + "." + SIZE,-1);
        }

        int seconds=Config.getIntProperty(CACHE + group + SECONDS, -1);
        if (seconds==-1) {
            seconds = Config.getIntProperty(CACHE + DEFAULT_CACHE + SECONDS, 100);
        }

        com.github.benmanes.caffeine.cache.stats.CacheStats cstats = foundCache.stats();
        stats.addStat(CacheStats.REGION, group);
        stats.addStat(CacheStats.REGION_DEFAULT, isDefault + "");
        stats.addStat(CacheStats.REGION_CONFIGURED_SIZE, SIZE + ":" + nf.format(size) + " / " + seconds + "s");
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

    @Override
    public void shutdown() {
        Logger.info(this.getClass(), "===== Calling shutdown [" + getName() + "].");
        isInitialized.set(false);
    }

    private Cache<String, Object> getCache(final String cacheName) {
        if (cacheName == null) {
            throw new DotStateException("Null cache region passed in");
        }
        return groups.computeIfAbsent(cacheName, k -> createCache(cacheName.toLowerCase()));
    }

    private Cache<String, Object> createCache(String cacheName) {
        Cache<String, Object> cache;
        boolean separateCache = (Config.getBooleanProperty(
                "cache.separate.caches.for.non.defined.regions", true)
                || availableCaches.contains(cacheName)
                || DEFAULT_CACHE.equals(cacheName));

        if (separateCache) {
            int size = Config.getIntProperty(CACHE + cacheName + "." + SIZE, -1);

            if (size == -1) {
                size = Config.getIntProperty(CACHE + DEFAULT_CACHE + "." + SIZE, 100);
            }

            final int defaultTTL = Config.getIntProperty(CACHE + cacheName + SECONDS, -1);


            Logger.debug(this.getClass(),
                    "***\t Building Cache : " + cacheName + ", " + SIZE + ":" + size
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

        } else {
            Logger.debug(this.getClass(),
                    "***\t No Cache for   : " + cacheName + ", using " + DEFAULT_CACHE);
            cache = getCache(DEFAULT_CACHE);
        }
        return cache;
    }
}
