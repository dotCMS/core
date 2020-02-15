package com.dotcms.cache.lettuce;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import com.dotmarketing.business.CacheLocator;
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
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.control.Try;


public class LettuceCache extends CacheProvider {
    private final LettuceClient lettuce;

    public LettuceCache(LettuceClient client) {
        lettuce = client;
    }

    public LettuceCache() {
        this(LettuceClient.getInstance());
    }
    
    private final AtomicBoolean allowGets = new AtomicBoolean(true);
    
    private static Timer timer;
    
    private final static String LETTUCE_GROUP_KEY="LETTUCE_GROUP_KEY";
    private final static String LETTUCE_PREFIX_KEY="LETTUCE_PREFIX_KEY";
    
    private final static boolean LETTUCE_PAUSE_GETS_ON_REMOVE =  Config.getBooleanProperty("redis.server.pause.gets.on.remove", false);
    
    
    final TimerTask timerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                Logger.info(this.getClass(), "\tLETTUCE GETS ALLOWED");
                allowGets.set(true);
            }
        };
    }
    
    
    private void resetGetTimer() {
        if(!LETTUCE_PAUSE_GETS_ON_REMOVE) {
            return;
        }
        allowGets.set(false);
        Try.run(()->timer.cancel());

        Timer timerNew = new Timer();
        timerNew.schedule(timerTask(), Config.getIntProperty("redis.server.pause.gets.timer.ms", 5000));
        timer = timerNew;
    }
    
    
    
    private static final long serialVersionUID = -855583393078878276L;
    private int keyBatching = Config.getIntProperty("redis.server.key.batch.size", 1000);
    private boolean isInitialized = false;
    final static AtomicLong prefixKey=new AtomicLong(0);

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
     * all key values that are put into redis are prefixed by a random key.
     * When a flushall is called, this key is cycled, which basically invalidates
     * all cached entries.  The prefix key is stored in redis itself so multiple 
     * servers in the cluster can read it.  When the key is cycled, we send a
     * CYCLE_KEY event to cluster to refresh the key across cluster 
     */
    @VisibleForTesting
    String loadPrefix() {
        long key = prefixKey.get();
        if(key==0) {
            prefixKey.set(setIfUnset(System.currentTimeMillis()));
        }

        return String.valueOf(prefixKey.get());
    }
    
    @VisibleForTesting
    long cycleKey() {
        long newKey = System.currentTimeMillis();
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().set(LETTUCE_PREFIX_KEY, newKey);
        }
        prefixKey.set(newKey);
        
        LettuceTransport transport = Try.of(()->(LettuceTransport) CacheLocator.getCacheAdministrator().getTransport()).getOrNull();
        if(transport!=null) {
            transport.send(MessageType.CYCLE_KEY, String.valueOf(newKey));
        }
        
        return newKey ;

    }
    
    @VisibleForTesting
    private long setIfUnset(long newKey) {

        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            return conn.sync().setnx(LETTUCE_PREFIX_KEY, newKey) ? newKey : (Long) conn.sync().get(LETTUCE_PREFIX_KEY);
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
    String cacheKey(String group, String key) {
        return loadPrefix() + 
                (group != null && key!=null ? "." + group + "." + key : (group!=null) ? "." + group + "." : "." );
    }
    
    @VisibleForTesting
    String cacheKey(String group) {
        return cacheKey(group, null);
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
        final String cacheKey = cacheKey(group, key);


        // Check if we must exclude this record from this cache
        if (exclude(cacheKey)) {
            return;
        }
        
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().sadd(LETTUCE_GROUP_KEY, group);

            conn.sync().set(cacheKey, content);
        }
        
    }

    @Override
    public Object get(String group, String key) {

        if (key == null || group == null || ! allowGets.get()) {
            return null;
        }
        final String cacheKey = cacheKey(group, key);

        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            return conn.sync().get(cacheKey.toString());
        }catch(RedisCommandTimeoutException rce) {
            Logger.warn(this.getClass(), "redis timout getting group:" + group + ", key:" + key);
            Logger.warnAndDebug(this.getClass(), rce);
        }
        return null;


    }

    /**
     * removes cache keys async and resets the get timer that 
     * reenables get functions
     * @param keys
     */
    private void remove(String ... keys) {
        if(keys.length==0) {
            return;
        }
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().unlink(keys);
        }
        resetGetTimer();
    }
    
    
    
    
    @Override
    public void remove(final String group, final String key) {

        final String cacheKey = cacheKey(group, key);

        this.remove(new String[] {cacheKey.toString()});


    }

    @Override
    public void remove(String group) {

        if (UtilMethods.isEmpty(group)) {
            return;
        }
        String prefix = cacheKey(group)+"*";
        // Getting all the keys for the given groups
        CacheWiper wiper = new CacheWiper(prefix);
        wiper.setName("CacheWiper prefix:" + prefix);
        wiper.start();

    }
    
    class CacheWiper extends Thread {


        public CacheWiper(String prefix) {
            this.prefix = prefix;
        }

        final String prefix;

        @Override
        public void run() {
            KeyScanCursor<String> scanCursor = null;
            Set<String> keys = new HashSet<String>();
            try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
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
    
    @Override
    public void removeAll() {
        

            final String prefix = cacheKey("*");
            cycleKey();
            // Getting all the keys for the given groups
            CacheWiper wiper = new CacheWiper(prefix);
            wiper.setName("CacheWiper prefix:" + prefix);
            wiper.start();
            
        
            
    }
    @Override
    public Set<String> getKeys(final String group) {
        final String prefix = cacheKey(group) +  "*";
        KeyScanCursor<String> scanCursor = null;

        Set<String> keys = new HashSet<String>();
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
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
        final String prefix = cacheKey(null) +  "*";
        KeyScanCursor<String> scanCursor = null;
        Set<String> keys = new HashSet<String>();
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
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

    private long keyCount(String group) {
        
        final String prefix = cacheKey(group)+"*";
        
        final String script = "return #redis.pcall('keys', '" + prefix + "')";
        
        Object j= null;
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            j= conn.sync().eval(script,ScriptOutputType.INTEGER, "0");
        }
        
        return (Long)j;
        
        
        
        
        
    }
    
    
    
    
    
    @Override
    public Set<String> getGroups() {
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            return conn.sync().smembers(LETTUCE_GROUP_KEY).stream().map(k->k.toString()).collect(Collectors.toSet());
        }
    }

    @Override
    public CacheProviderStats getStats() {
        CacheStats providerStats = new CacheStats();
        CacheProviderStats ret = new CacheProviderStats(providerStats, getName());
        String memoryStats = null;
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            memoryStats = conn.sync().info();
        }

        // Read the total memory usage
        
        Map<String,String> redis = getRedisProperties(memoryStats);

        for(Map.Entry<String, String> entry : redis.entrySet()) {
            CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, "redis: " + entry.getKey());
            stats.addStat(CacheStats.REGION_SIZE, entry.getValue());
            ret.addStatRecord(stats);
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
    private Map<String,String> getRedisProperties(String redisReport) {

        Map<String,String> map = new LinkedHashMap<>();

        String[] readLines = redisReport.split("\r\n");
        for (String readLine : readLines) {

            String[] lineValues = readLine.split(":",2);

            // First check if it is a property or a header
            if (lineValues.length > 1) {

                map.put(lineValues[0], lineValues[1]);
                
            }
        }

        return map;
    }



    /**
     * Method that verifies if must exclude content that can not or must not be added to this Redis
     * cache based on the given cache group and key
     *
     * @param group
     * @param key
     * @return
     */
    private boolean exclude(String key) {

        boolean exclude = false;

        if (key.equals(ONLY_MEMORY_GROUP)) {
            exclude = true;

            cannotCacheCache.put(key.toString(), key.toString());
        }

        return exclude;
    }

}
