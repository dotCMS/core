package com.dotcms.graphql;

import com.dotcms.cache.Expirable;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.logging.log4j.util.Strings;

/**
 * A Expiring cache implementation to store the results of GraphQL requests.
 *
 * The amount of time the results will be cached can be specified either by providing a TTL (int)
 * by calling {@link #put(String, String, long)} method or by the config property <code>cache.graphqlquerycache.seconds</code>
 * which applies when calling {@link #put(String, String)}, which does not take a TTL
 *
 * This entire cache can be turned off by setting GRAPHQL_CACHE_RESULTS=false in the dotmarketing-config.properties
 *
 */
public class GraphQLCache implements Cachable {
    private final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    public static final String GRAPHQL_CACHE_RESULTS_CONFIG_PROPERTY = "GRAPHQL_CACHE_RESULTS";

    private final Lazy<Boolean> ENABLED_FROM_CONFIG = Lazy.of(()->Config
            .getBooleanProperty(GRAPHQL_CACHE_RESULTS_CONFIG_PROPERTY, true));

    /**
     * Gets the value from cache, if any and non-expired, for the given key.
     * Optionally it can be instructed to refresh the value for the key in the background only if
     * cache hit, in which case the cached value is always returned, even if expired.
     * @param key cache key
     * @param valueSupplier (only applies when refresh=true) supplier for the value
     * @param ttl (only applies when refresh=true) time to cache the entry for - if null, then it
     * will cache for the time specified in the config property
     * <code>cache.graphqlquerycache.seconds</code>
     * @return optional of the cached value
     */

    private Optional<String> get(final String key,
            final Supplier<String> valueSupplier, final long ttl) {
        if(cannotCache()) {
            return Optional.empty();
        }

        final String cacheKey = hashKey(key);
        final Object value = cache.getNoThrow(cacheKey, getPrimaryGroup());


        refreshKey(key, valueSupplier, ttl);

        return value instanceof ExpirableCacheEntry
                ? Optional.ofNullable(((ExpirableCacheEntry) value).getResults())
                : Optional.ofNullable((String)value);
    }

    /**
     * Gets the value from cache, if any and non-expired, for the given key.
     * @param key cache key
     * @return optional of the cached value
     */

    public Optional<String> get(final String key) {
        return get(key, null, -1);
    }

    /**
     * Same as {@link #get(String)} but loads a new value for the key asynchronously if cache hit
     * @param key cache key
     * @param valueSupplier supplier for the value
     * @param ttl time to cache the entry for - if null, then it will cache for the time specified in the config property
     * <code>cache.graphqlquerycache.seconds</code>
     * @return optional of the cached value
     */

    public Optional<String> getAndRefresh(final String key, final Supplier<String> valueSupplier,
            final Integer ttl) {
        return get(key, valueSupplier, ttl);
    }

    private void refreshKey(final String key, final Supplier<String> valueSupplier,
            final long cacheTTL) {
        if(valueSupplier==null) return;

        DotConcurrentFactory.getInstance()
                .getSingleSubmitter().submit(()-> {
                        remove(key);
                        put(key, Try.of(valueSupplier::get).getOrElse(Strings.EMPTY), cacheTTL);
                });
    }

    /**
     * Puts into cache the value and expired time, based on the given ttl,  for the given key
     * @param key the key
     * @param result the value
     * @param ttl time in seconds to consider the value as expired. If null, then it will cache
     * the value for the time specified in the config property <code>cache.graphqlquerycache.seconds</code>
     */
    public void put(String key, String result, long ttl) {
        if(cannotCache()) return;

        if(UtilMethods.isNotSet(result)) return;

        final String cacheKey = hashKey(key);
        cache.put(cacheKey, new ExpirableCacheEntry(result, ttl), getPrimaryGroup());
    }

    /**
     * Puts into cache the value for the given key for the time specified in the config
     * property <code>cache.graphqlquerycache.seconds</code>
     * @param key the key
     * @param result the value
     */
    public void put(String key, String result) {
        this.put(key, result, -1);
    }

    private String hashKey(final String query) {
        long hashCode = 1125899906842597L;
        for (int i = 0; i < query.length(); i++) {
            hashCode = 31 * hashCode + query.charAt(i);
        }

        return String.valueOf(hashCode);
    }

    @Override
    public void clearCache() {
        cache.flushGroup(getPrimaryGroup());
    }

    @Override
    public String[] getGroups() {
        return new String[] {getPrimaryGroup()};
    }

    @Override
    public String getPrimaryGroup() {
        return "graphqlquerycache";
    }

    private boolean cannotCache() {
        return !LicenseManager.getInstance().isEnterprise() || !ENABLED_FROM_CONFIG.get();
    }

    public void remove(final String key) {
        final String cacheKey = hashKey(key);
        this.cache.remove(cacheKey, getPrimaryGroup());
    }

    private static class ExpirableCacheEntry implements Expirable, Serializable {
        private static final long serialVersionUID = 1L;
        private final long ttl;
        private final String results;

        public ExpirableCacheEntry(String results, long ttl) {
            this.results = results;
            this.ttl = ttl;
        }

        public long getTtl() {
            return ttl;
        }

        public String getResults() {
            return results;
        }
    }

}
