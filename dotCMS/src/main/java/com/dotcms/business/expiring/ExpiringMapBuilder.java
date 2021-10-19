package com.dotcms.business.expiring;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.concurrent.TimeUnit;

/**
 * Builder for {@link ExpiringMap}
 * @param <K>
 * @param <V>
 */
public class ExpiringMapBuilder<K,V> {

    private long ttl    = Config.getLongProperty("dotcms.expiringmap.default.globalttl", DateUtil.MINUTE_MILLIS);
    private int  size   = 100;
    private ExpiringEntryStrategy<K,V> expiringEntryStrategy =
            (key, value)-> Tuple.of(Config.getLongProperty("dotcms.expiringmap.default.entryttl", DateUtil.THREE_SECOND_MILLIS), TimeUnit.MILLISECONDS);

    /**
     * Set the strategy for expiring entries on the map, by default the strategy will return 3 seconds for all entry.
     *
     * See #ExpiringEntryStrategy
     * @param expiringEntryStrategy
     * @return
     */
    public ExpiringMapBuilder expiringEntryStrategy(final ExpiringEntryStrategy<K,V> expiringEntryStrategy) {

        this.expiringEntryStrategy = expiringEntryStrategy;
        return this;
    }

    /**
     * Set the global ttl for the Map, it is not the same of the entry ttl, it is the max time for all entries to live in the Map
     * by default is a minute.
     * @param millis {@link Long} time in milli seconds
     * @return ExpiringMapBuilder
     */
    public ExpiringMapBuilder ttl(final long millis) {

        this.ttl = millis;
        return this;
    }

    /**
     * Size of the Map, by default 100
      * @param size {@link Integer}
     * @return ExpiringMapBuilder
     */
    public ExpiringMapBuilder size(final int size) {

        this.size = size;
        return this;
    }

    public ExpiringMap<K,V> build () {

        final Cache<K, V> cache =
                Caffeine.newBuilder()
                .maximumSize(size)
                .expireAfterWrite(this.ttl, TimeUnit.MILLISECONDS)
                .build();
        return new ExpiringMapImpl<>(cache, this.expiringEntryStrategy);
    }

    private final class ExpiringMapImpl<K,V> implements ExpiringMap<K,V> {

        private final ExpiringEntryStrategy<K,V> strategy;
        private final Cache<K, V> cache;

        public ExpiringMapImpl(final Cache<K, V> cache, final ExpiringEntryStrategy<K,V> strategy) {
            this.cache    = cache;
            this.strategy = strategy;
        }

        @Override
        public ExpiringEntryStrategy<K, V> getExpiringEntryStrategy() {
            return strategy;
        }

        @Override
        public V put(final K key, final V value, final long ttl, final TimeUnit unit) {

            this.cache.put(key, value);

            // will kill it from the cache in some time.
            final DotSubmitter submitter = DotConcurrentFactory.getInstance().getSubmitter();
            submitter.delay(()-> this.remove(key), ttl, unit);

            return value;
        }

        @Override
        public V put(final K key, final V value, final boolean useCacheTtl) {
           if(useCacheTtl){
               this.cache.put(key, value);
               return value;
           } else {
              final Tuple2<Long, TimeUnit> expiring = this.strategy.getExpireTime(key, value);
              return this.put(key, value, expiring._1, expiring._2);
           }
        }

        @Override
        public V put(final K key, final V value) {
            return this.put(key, value, false);
        }

        @Override
        public boolean containsKey(final K key) {
            return null != this.get(key);
        }

        @Override
        public V remove(final K key) {
            final V value = this.get(key);
            this.cache.invalidate(key);
            return value;
        }

        @Override
        public V get(final K key) {
            return this.cache.getIfPresent(key);
        }
    }
}
