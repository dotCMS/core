package com.dotcms.cache.lettuce;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
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
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;


public class LettuceCache extends CacheProvider {
    private final LettuceClient<String, Object> lettuce;

    private final String clusterId;
    
    
    public LettuceCache(LettuceClient<String,Object> client, final String clusterId) {
        lettuce = client;
        this.clusterId = clusterId;
        this.LETTUCE_GROUP_KEY = clusterId + "LETTUCE_GROUP_KEY";
        this.LETTUCE_PREFIX_KEY = clusterId + "LETTUCE_PREFIX_KEY";
    }

    public LettuceCache() {
        this(new MasterReplicaLettuceClient<>(), APILocator.getShortyAPI().shortify(ClusterFactory.getClusterId()));
    }

    private final static String PREFIX_UNSET="PREFIX_UNSET";


    private final String LETTUCE_GROUP_KEY;
    private final String LETTUCE_PREFIX_KEY;

    private final long dirtyReadDelayMs = Config.getLongProperty("redis.lettucecache.dirtyread.delay.ms", 10000L);
    private final long dirtyReadCacheSize = Config.getLongProperty("redis.lettucecache.dirtyread.size", 100000L);

    /**
     * The dirtyReadCache caches every write (put/remove) sent to Redis and will not allow a get for
     * that key until 10 seconds has passed. This allows us to do async writes with confidence.
     */
    private final Cache<String, Boolean> dirtyReadCache = Caffeine.newBuilder().maximumSize(dirtyReadCacheSize)
                    .expireAfterWrite(dirtyReadDelayMs, TimeUnit.MILLISECONDS).build();



    private static final long serialVersionUID = -855583393078878276L;
    private int keyBatching = Config.getIntProperty("redis.server.key.batch.size", 1000);
    final static AtomicReference<String> prefixKey = new AtomicReference(PREFIX_UNSET);


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
            key = loadPrefixFromRedis(); 
        }
        if (PREFIX_UNSET.equals(key)) {
            key = setIfUnset();
        }
        if (PREFIX_UNSET.equals(key)) {
            Logger.warn(this.getClass(), "unable to determine key prefix");
            key = PREFIX_UNSET;
        }
        prefixKey.set(key);
        return key;
    }
    
    String loadPrefixFromRedis() {
        try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
            return (String) lettuce.get().sync().get(LETTUCE_PREFIX_KEY);
        } catch (Exception e) {
            Logger.debug(this.getClass(), ()-> "unable to get prefix:" + e.getMessage());
            return PREFIX_UNSET;
        }
    }
    
    
    String generateNewKey() {
        return clusterId + "_" + new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss").format(new Date());
    }

    /**
     * This will forceably cycle the prefix key, effectivily wiping the cache.
     * 
     * @return
     */
    @VisibleForTesting
    String cycleKey() {
        final String newKey = generateNewKey();
        try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
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
     * @param newKey
     * @return
     */
    @VisibleForTesting
    String setIfUnset() {
        final String newKey = generateNewKey();
        try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
            if (!conn.isOpen()) {
                return PREFIX_UNSET;
            }
            return conn.sync().setnx(LETTUCE_PREFIX_KEY, newKey) ? newKey : loadPrefixFromRedis();
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
        return loadPrefix()
                        + (group != null && key != null ? "." + group + "." + key : (group != null) ? "." + group + "." : ".");
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
    public void put(final String group, final String key, Object content) {

        if (key == null || group == null) {
            return;
        }
        final String cacheKey = cacheKey(group, key);
        dirtyReadCache.put(cacheKey, true);

        try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
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

        if (dirtyReadCache.get(cacheKey(group), k -> {
            return false;
        })) {
            return null;
        }

        try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
            if (conn.isOpen()) {
                return conn.sync().get(cacheKey.toString());
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
        for (String key : keys) {
            dirtyReadCache.put(key, true);
        }
        try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
            if (conn.isOpen()) {
                conn.async().unlink(keys);
            }
        }

    }



    @Override
    public void remove(final String group, final String key) {

        final String cacheKey = cacheKey(group, key);

        this.remove(new String[] {cacheKey.toString()});


    }

    @Override
    public void remove(final String group) {

        if (UtilMethods.isEmpty(group)) {
            return;
        }
        dirtyReadCache.put(cacheKey(group), true);
        String prefix = cacheKey(group) + "*";
        // Getting all the keys for the given groups
        CacheWiper wiper = new CacheWiper(prefix);

        DotConcurrentFactory.getInstance().getSingleSubmitter(CacheWiper.class.getSimpleName()).submit(wiper);

    }

    class CacheWiper  implements Runnable{

        @Override
        public String toString() {
            return "CacheWiper prefix:" + prefix;
        }

        public CacheWiper(final String prefix) {
            this.prefix = prefix;
        }

        final String prefix;

        @Override
        public void run() {
            KeyScanCursor<String> scanCursor = null;

            try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
                if (!conn.isOpen()) {
                    return;
                }
                do {
                    if (scanCursor == null) {
                        scanCursor = conn.sync().scan(ScanArgs.Builder.matches(prefix).limit(keyBatching));
                    } else {
                        scanCursor = conn.sync().scan(scanCursor, ScanArgs.Builder.matches(prefix).limit(keyBatching));
                    }
                    remove(scanCursor.getKeys().toArray(new String[0]));


                } while (!scanCursor.isFinished());
            }

        }
    };

    class PrefixChecker  implements Runnable{

        @Override
        public void run() {


            final String ourPrefix =  prefixKey.get();
            final String redisPrefix = loadPrefixFromRedis();
            // force a reload if needed
            if(!Objects.equal(ourPrefix, redisPrefix)) {
                prefixKey.set(PREFIX_UNSET);
            }
            

        }
    };
    
    
    
    
    @Override
    public void removeAll() {


        final String prefix = cacheKey("*");
        cycleKey();
        // Getting all the keys for the given groups
        CacheWiper wiper = new CacheWiper(prefix);
        DotConcurrentFactory.getInstance().getSingleSubmitter(CacheWiper.class.getSimpleName()).submit(wiper);



    }

    @Override
    public Set<String> getKeys(final String group) {
        final String prefix = cacheKey(group) + "*";
        KeyScanCursor<String> scanCursor = null;

        Set<String> keys = new HashSet<String>();
        try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
            if (!conn.isOpen()) {
                return new HashSet<>();
            }
            do {
                if (scanCursor == null) {
                    scanCursor = conn.sync().scan(ScanArgs.Builder.matches(prefix).limit(keyBatching));
                } else {
                    scanCursor = conn.sync().scan(scanCursor, ScanArgs.Builder.matches(prefix).limit(keyBatching));
                }


                keys.addAll(scanCursor.getKeys());

            } while (!scanCursor.isFinished());
        }

        return keys;


    }


    @VisibleForTesting
    Set<String> getAllKeys() {
        final String prefix = cacheKey(null) + "*";
        KeyScanCursor<String> scanCursor = null;
        Set<String> keys = new HashSet<String>();
        try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
            if (!conn.isOpen()) {
                return new HashSet<>();
            }
            do {
                if (scanCursor == null) {
                    scanCursor = conn.sync().scan(ScanArgs.Builder.matches(prefix).limit(keyBatching));
                } else {
                    scanCursor = conn.sync().scan(scanCursor, ScanArgs.Builder.matches(prefix).limit(keyBatching));
                }


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

        Object j = null;
        try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
            if (!conn.isOpen()) {
                return 0;
            }
            j = conn.sync().eval(script, ScriptOutputType.INTEGER, "0");
        }

        return (Long) j;



    }



    @Override
    public Set<String> getGroups() {
        try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
            if (!conn.isOpen()) {
                return new HashSet<>();
            }
            return conn.sync().smembers(LETTUCE_GROUP_KEY).stream().map(k -> k.toString()).collect(Collectors.toSet());
        }
    }

    @Override
    public CacheProviderStats getStats() {
        CacheStats providerStats = new CacheStats();
        CacheProviderStats ret = new CacheProviderStats(providerStats, getName());
        String memoryStats = null;
        try (StatefulRedisConnection<String,Object> conn = lettuce.get()) {
            if (!conn.isOpen()) {
                return ret;
            }
            memoryStats = conn.sync().info();
        }

        // Read the total memory usage

        Map<String, String> redis = getRedisProperties(memoryStats);

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

            ret.addStatRecord(stats);
        }

        return ret;
    }

    @Override
    public void shutdown() {

        Logger.info(this.getClass(), "*** Shutdown [" + getName() + "] .");
        prefixKey.set(PREFIX_UNSET);
    }

    /**
     * Reads and parses the string report generated for the INFO Redis command in order to return any
     * specific required property.
     *
     * @param redisReport
     * @param property
     * @return
     */
    private Map<String, String> getRedisProperties(final String redisReport) {

        Map<String, String> map = new LinkedHashMap<>();

        String[] readLines = redisReport.split("\r\n");
        for (String readLine : readLines) {

            String[] lineValues = readLine.split(":", 2);

            // First check if it is a property or a header
            if (lineValues.length > 1) {

                map.put(lineValues[0], lineValues[1]);

            }
        }

        return map;
    }



}
