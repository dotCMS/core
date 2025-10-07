package com.dotcms.cache;


import com.dotmarketing.util.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;


/**
 * This class constructs a cache with dynamic TTLs for each key.
 * Relies on Caffeine for the underlying cache implementation.
 * @param <K>
 * @param <V>
 */
public class DynamicTTLCache<K, V>  {

    public final long defaultTTLInMillis;
    private final Cache<K, CacheValue> cache;

    public DynamicTTLCache(long maxCapacity) {
        this(maxCapacity, Long.MAX_VALUE);
    }


    public DynamicTTLCache(long maxCapacity, long defaultTTLInMillis) {
        this.defaultTTLInMillis = defaultTTLInMillis;
        this.cache = Caffeine.newBuilder()
            .initialCapacity((int) maxCapacity)
            .expireAfter(new Expiry<K, CacheValue>() {
                @Override
                public long expireAfterCreate(@NotNull K key, @NotNull CacheValue value, long currentTime) {
                    return TimeUnit.MILLISECONDS.toNanos(value.getTtlInMillis());
                }

                @Override
                public long expireAfterUpdate(K key, CacheValue value, long currentTime,
                        long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(K key, CacheValue value, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .recordStats()
                .evictionListener((key, value, cause) -> {
                    switch (cause) {
                        case SIZE:
                            Logger.debug(this.getClass(), () -> "TTLCache entry evicted due to size: " + key);
                            break;
                        case EXPIRED:
                            Logger.debug(this.getClass(), () -> "TTLCache entry evicted due to expired: " + key);
                            break;
                        case REPLACED:
                            Logger.debug(this.getClass(), () -> "TTLCache entry evicted due to replaced: " + key);
                            break;

                    }

                })
            .maximumSize(maxCapacity)
            .build();
    }

    public void put(K key, V value, long ttlInMillis) {

        if(value instanceof CacheValue) {
            cache.put(key, (CacheValue) value);
        }else{
            cache.put(key, new CacheValueImpl(value, ttlInMillis));
        }

    }

    public void put(K key, V value) {
        this.put(key, value, defaultTTLInMillis);
    }

    public V getIfPresent(K key) {
        CacheValue cacheValue = cache.getIfPresent(key);

        return cacheValue != null ? (V) cacheValue.getValue() : null;
    }


    public void invalidate(K key) {
        cache.invalidate(key);
    }

    public void invalidateAll() {

        cache.invalidateAll();
    }

    public CacheStats stats() {
        return cache.stats();
    }

    public long estimatedSize() {
        return cache.estimatedSize();
    }

    public Map<K, CacheValue> asMap() {
        return cache.asMap();
    }

    public Map<K,V> copyAsMap() {
        return cache.asMap().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> (V) e.getValue().getValue()));
    }


}
