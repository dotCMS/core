package com.dotmarketing.business.cache.provider.timedcache;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * @author Jonathan Gamba Date: 9/2/15
 */
public class TimedCacheProvider extends CacheProvider {


	private static final long serialVersionUID = 1L;

	private Boolean isInitialized = false;

	private final ConcurrentHashMap<String, Cache<String, Object>> groups = new ConcurrentHashMap<>();

	static final String DEFAULT_CACHE = CacheProviderAPI.DEFAULT_CACHE;
	static final String LIVE_CACHE_PREFIX = CacheProviderAPI.LIVE_CACHE_PREFIX;
	static final String WORKING_CACHE_PREFIX = CacheProviderAPI.WORKING_CACHE_PREFIX;


	private final HashSet<String> availableCaches = new HashSet<>();

	private final int DEFAULT_TIMEOUT = 100;

	@Override
	public String getName() {
		return "Timed Cache Provider";
	}

	@Override
	public String getKey() {
		return "Timed Cache Provider";
	}

	@Override
    public boolean isDistributed() {
    	return false;
    }

	@Override
	public void init() {
		groups.clear();
		Logger.info(this.getClass(), "===== Initializing [" + getName() + "].");
		availableCaches.add(DEFAULT_CACHE);
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
					availableCaches.add(cacheName.toLowerCase());
					Logger.info(this.getClass(), "***\t Cache Config Memory : "
							+ cacheName + ": " + inMemory);
				}
			}
		}

		isInitialized = true;
	}

	@Override
	public boolean isInitialized () throws Exception {
		return isInitialized;
	}

	@Override
	public void put(String group, String key, Object content) {
		if (group == null || key == null || content == null) {
			return;
		}
		Cache<String, Object> cache = getCache(group);
		cache.put(key, content);
	}

	@Override
	public synchronized Object get(String group, String key) {
		// Get the cache for the given group
		Cache cache = getCache(group);
		return cache.getIfPresent(key);

	}

	@Override
	public void remove(String group, String key) {

		Cache<String, Object> cache = getCache(group);
		// Invalidates from Cache a key from a given group
		cache.invalidate(key);
	}

	@Override
	public void remove(String group) {
		Logger.debug(this.getClass(), "===== Calling remove for [" + getName()
				+ "] - " + cacheKey(group, ""));
		// Get the cache for the given group
		Cache<String, Object> cache = getCache(group);
		// Invalidates the Cache for the given group
		cache.invalidateAll();
		// Remove this group from the global list of cache groups
		groups.remove(group);
	}

	@Override
	public void removeAll() {
		this.init();
	}

	@Override
	public Set<String> getKeys(String group) {
		Cache<String, Object> cache = getCache(group);
		return cache.asMap().keySet();
	}

	@Override
	public Set<String> getGroups() {
		return groups.keySet();
	}

    @Override
    public CacheProviderStats getStats() {

        CacheStats providerStats = new CacheStats();
        CacheProviderStats ret = new CacheProviderStats(providerStats,getName());

        Set<String> currentGroups = new HashSet<>();
        currentGroups.addAll(getGroups());

        NumberFormat nf = DecimalFormat.getInstance();
        DecimalFormat pf = new DecimalFormat("##.##%");
        for (String group : currentGroups) {
            CacheStats stats = new CacheStats();

            Cache<String, Object> foundCache = getCache(group);


            boolean isDefault = (Config.getIntProperty("cache." + group + ".size", -1) == -1 && Config.getIntProperty("cache." + group + ".seconds", -1) == -1);


            int size = isDefault ? Config.getIntProperty("cache." + DEFAULT_CACHE + ".size", 100)
                : (Config.getIntProperty("cache." + group + ".size", -1) != -1)
                  ? Config.getIntProperty("cache." + group + ".size")
                      : Config.getIntProperty("cache." + DEFAULT_CACHE + ".size", 100);

              int seconds = isDefault ? Config.getIntProperty("cache." + DEFAULT_CACHE + ".seconds", 100)
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
            

            ret.addStatRecord(stats);
        }
        
        if(currentGroups.isEmpty()) {
            CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, "n/a");
            stats.addStat(CacheStats.REGION_SIZE, 0);
            ret.addStatRecord(stats);
        }

        return ret;
    }

	@Override
	public void shutdown() {
		Logger.info(this.getClass(), "===== Calling shutdown [" + getName()+ "].");
		isInitialized = false;
	}

	private String cacheKey(String group, String key) {
		return (group + ":" + key).toLowerCase();
	}

	private synchronized Cache<String, Object> getCache(String cacheName) {
		if (cacheName == null) {
			throw new DotStateException("Null cache region passed in");
		}
		cacheName = cacheName.toLowerCase();
		Cache<String, Object> cache = groups.get(cacheName);
		// init cache if it does not exist
		if (cache == null) {
			synchronized (cacheName.intern()) {
				cache = groups.get(cacheName);
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
                                int seconds = Config.getIntProperty("cache." + cacheName + ".seconds", -1);
		                        if (size == -1) {
		                            size = Config.getIntProperty("cache." + DEFAULT_CACHE + ".size", 100);
		                        }
                                if (seconds == -1) {
                                    seconds = Config.getIntProperty("cache." + DEFAULT_CACHE + ".seconds", 100);
                                }
		                        Logger.infoEvery(this.getClass(),
		                                "***\t Building Cache : " + cacheName + ", size:" + size +  ", seconds:" + seconds 
		                                        + ",Concurrency:"
		                                        + Config.getIntProperty("cache.concurrencylevel", 32), 60000);
		                        cache = Caffeine.newBuilder()
		                                .maximumSize(size)
		                                .expireAfterWrite(seconds, TimeUnit.SECONDS)
		                                .recordStats()
		                                //.softValues()
		                                .build();


		                        groups.put(cacheName, cache);

		                    } else {
		                        Logger.infoEvery(this.getClass(),
		                                "***\t No Cache for   : " + cacheName + ", using " + DEFAULT_CACHE, 60000);
		                        cache = getCache(DEFAULT_CACHE);
		                        groups.put(cacheName, cache);
		                    }
		                }
		            }
		        }
	
			}
		}
		return cache;
	}



}