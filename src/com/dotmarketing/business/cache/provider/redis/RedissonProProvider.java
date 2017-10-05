package com.dotmarketing.business.cache.provider.redis;

import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.util.Logger;
import org.redisson.Redisson;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RedissonProProvider extends CacheProvider {

    boolean initialized = false;
    private ConcurrentHashMap<String, RLocalCachedMap<String,Object>> lgroups = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, RMap<String,Object>> rgroups = new ConcurrentHashMap<>();
    private List<String> noLocalCache = new ArrayList<>();
    private RedissonClient redissonClient;

    static final String DEFAULT_CACHE = CacheProviderAPI.DEFAULT_CACHE;
    static final String LIVE_CACHE_PREFIX = CacheProviderAPI.LIVE_CACHE_PREFIX;
    static final String WORKING_CACHE_PREFIX = CacheProviderAPI.WORKING_CACHE_PREFIX;

    @Override
    public void init() throws Exception {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("redisson-pro.yaml");
            Config config = Config.fromYAML(is);
            init(config);
        }catch(Exception e){
            Logger.error(this,"Unable to start Redisson Pro Provider", e);
            throw e;
        }

        initialized=true;
    }

    protected void init(Config config) throws  Exception {
        redissonClient = Redisson.create(config);
    }

    private RLocalCachedMap<String,Object> getLocalCache(String cacheName){
        if ( cacheName == null ) {
            throw new DotStateException("Null cache region passed in");
        }

        if(noLocalCache.contains(cacheName)) {
            return null;
        }

        cacheName = cacheName.toLowerCase();
        RLocalCachedMap<String, Object> lcache = lgroups.get(cacheName);

        // init cache if it does not exist
        if ( lcache == null ) {
            synchronized (cacheName.intern()) {
                lcache = lgroups.get(cacheName);
                if(noLocalCache.contains(cacheName)) {
                    return null;
                }

                if ( lcache == null ) {
                    LocalCachedMapOptions cacheOptions = populateConfig(cacheName);
                    if(cacheOptions == null){
                        noLocalCache.add(cacheName);
                        return null;
                    }

                    Logger.info(this.getClass(),"***\t Building Cache : " + cacheName + " with the following options");
                    Logger.info(this.getClass(), "evictionpolicy is set to " + cacheOptions.getEvictionPolicy());
                    Logger.info(this.getClass(), "cachesize is set to " + cacheOptions.getCacheSize());
                    Logger.info(this.getClass(), "invalidationpolicy is set to " + cacheOptions.getInvalidationPolicy());
                    Logger.info(this.getClass(), "maxidle is set to " + cacheOptions.getMaxIdleInMillis());
                    Logger.info(this.getClass(), "timetolive is set to " + cacheOptions.getTimeToLiveInMillis());
                    lcache = redissonClient.getLocalCachedMap(cacheName, cacheOptions);
                    lgroups.put(cacheName,lcache);
                    Logger.info(this.getClass(),"***\t Finished Building Cache : " + cacheName);
                }
            }
        }
        return lcache;
    }

    private RMap<String, Object> getRemoteCache(String cacheName){
        if ( cacheName == null ) {
            throw new DotStateException("Null cache region passed in");
        }

        cacheName = cacheName.toLowerCase();
        RMap<String, Object> rcache = rgroups.get(cacheName);
        if ( rcache == null ) {
            synchronized (cacheName.intern()) {
                rcache = lgroups.get(cacheName);
                if ( rcache == null ) {
                    rcache =  redissonClient.getMap(cacheName);
                }
            }
        }
        return rcache;
    }

    private LocalCachedMapOptions populateConfig(String cacheName){
        LocalCachedMapOptions options = LocalCachedMapOptions.defaults();

        //Get String Values for all possible properties
        String evictionPolicy = getConfigProperty(cacheName,"evictionpolicy", "lfu");
        String cacheSize = getConfigProperty(cacheName,"cachesize", "1000");
        String invalidationpolicy = getConfigProperty(cacheName,"invalidationpolicy", "ON_CHANGE_WITH_CLEAR_ON_RECONNECT");
        String maxidle = getConfigProperty(cacheName,"maxidle", "100");
        String timetolive = getConfigProperty(cacheName,"timetolive", "100");

        if(cacheSize.equalsIgnoreCase("-1")){
            return null;
        }

        //Setup Eviction Policy
        if(evictionPolicy.equalsIgnoreCase("lfu")){
            options.evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU);
        } else if (evictionPolicy.equalsIgnoreCase("lru")){
            options.evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU);
        } else if (evictionPolicy.equalsIgnoreCase("soft")){
            options.evictionPolicy(LocalCachedMapOptions.EvictionPolicy.SOFT);
        } else {
            options.evictionPolicy(LocalCachedMapOptions.EvictionPolicy.NONE);
        }

        //Setup Invalidation policy
        if(invalidationpolicy.equalsIgnoreCase("ON_CHANGE_WITH_CLEAR_ON_RECONNECT")){
            options.invalidationPolicy(LocalCachedMapOptions.InvalidationPolicy.ON_CHANGE_WITH_CLEAR_ON_RECONNECT);
        } else if (invalidationpolicy.equalsIgnoreCase("ON_CHANGE_WITH_LOAD_ON_RECONNECT")){
            options.invalidationPolicy(LocalCachedMapOptions.InvalidationPolicy.ON_CHANGE_WITH_LOAD_ON_RECONNECT);
        } else{
            options.invalidationPolicy(LocalCachedMapOptions.InvalidationPolicy.ON_CHANGE);
        }

        //Setup Cache Size
        options.cacheSize(new Integer(cacheSize));

        //Setup Max Idle Time
        options.maxIdle(new Integer(maxidle), TimeUnit.SECONDS);

        //Setup Time To Live
        options.timeToLive(new Integer(timetolive), TimeUnit.SECONDS);

        return options;
    }

    private String getConfigProperty(String cacheName, String cacheProperty, String defaultValue){
        String value = "default";
        if ( cacheName.startsWith(LIVE_CACHE_PREFIX) ) {
            value = com.dotmarketing.util.Config.getStringProperty("cache." + cacheName + "." + cacheProperty, "default").trim();
            if(value.equals("default")) {
                value = com.dotmarketing.util.Config.getStringProperty("cache." + LIVE_CACHE_PREFIX + "." + cacheProperty, "default").trim();
            }
        } else if ( cacheName.startsWith(WORKING_CACHE_PREFIX) ) {
            value = com.dotmarketing.util.Config.getStringProperty("cache." + cacheName + "." + cacheProperty, "default").trim();
            if(value.equals("default")) {
                value = com.dotmarketing.util.Config.getStringProperty("cache." + WORKING_CACHE_PREFIX + "." + cacheProperty, "default").trim();
            }
        } else {
            value = com.dotmarketing.util.Config.getStringProperty("cache." + cacheName + "." + cacheProperty, "default").trim();
        }

        if (value.equals("default")) {
            value = com.dotmarketing.util.Config.getStringProperty("cache." + DEFAULT_CACHE + "." + cacheProperty, defaultValue);
        }

        return value;
    }

    private RMap<String, Object> getCacheMap(String cacheName){
        RMap<String, Object> cacheMap = null;
        cacheMap = getLocalCache(cacheName);
        if(cacheMap == null){
            cacheMap = getRemoteCache(cacheName);
        }
        return cacheMap;
    }

    @Override
    public String getName() {
        return "Redisson Pro";
    }

    @Override
    public String getKey() {
        return "redissonPro";
    }

    @Override
    public boolean isInitialized() throws Exception {
        return initialized;
    }

    @Override
    public void put(String group, String key, Object content) {
        getCacheMap(group).fastPut(key, content);
    }

    @Override
    public Object get(String group, String key) {
        return getCacheMap(group).get(key);
    }

    @Override
    public void remove(String group, String key) {
        getCacheMap(group).fastRemove(key);
    }

    @Override
    public void remove(String group) {
        getCacheMap(group).delete();
    }

    @Override
    public void removeAll() {
        Object[] caches = CacheLocator.getCacheIndexes();
        for (int i = 0; i<caches.length; i++) {
            getCacheMap(caches[i].toString()).delete();
        }
    }

    @Override
    public Set<String> getKeys(String group) {
        return new HashSet<String>(getCacheMap(group).keySet());
    }

    @Override
    public Set<String> getGroups() {
        Set<String> groups = new HashSet<>();
        groups.addAll(rgroups.keySet());
        groups.addAll(lgroups.keySet());
        return groups;
    }

    @Override
    public List<Map<String, Object>> getStats() {
        List<Map<String, Object>> list = new ArrayList<>();
        Object[] caches = CacheLocator.getCacheIndexes();
        for (int i = 0; i<caches.length; i++) {
            RMap<String, Object> rmap = getCacheMap(caches[i].toString());
            RLocalCachedMap<String, Object> lcache = lgroups.get(caches[i].toString());
            Map<String, Object> stats = new HashMap<>();
            stats.put("name", getName());
            stats.put("key", getKey());
            stats.put("region", caches[i].toString());
            stats.put("toDisk", true);

            stats.put("isDefault", false);
            stats.put("memory", lcache.size());
            stats.put("disk", rmap.size());
            stats.put("configuredSize", getConfigProperty(caches[i].toString(),"cachesize","default"));

            list.add(stats);
        }
        return list;
    }

    @Override
    public void shutdown() {
        redissonClient.shutdown();
    }
}
