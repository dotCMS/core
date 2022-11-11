package com.dotcms.rendering.velocity.viewtools.dotcache;


import java.io.Serializable;
import java.util.Optional;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Logger;

/**
 * A Expiring cache implementation to store the results of GraphQL requests.
 *
 * The amount of time the results will be cached can be specified either by providing a TTL (int) by
 * calling {@link #put(String, String, long)} method or by the config property
 * <code>cache.graphqlquerycache.seconds</code> which applies when calling
 * {@link #put(String, String)}, which does not take a TTL
 *
 * This entire cache can be turned off by setting GRAPHQL_CACHE_RESULTS=false in the
 * dotmarketing-config.properties
 *
 */
class DotVelocityCache implements Cachable {
    private final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    private final ExpirableCacheEntry EMPTY_ENTRY  = new ExpirableCacheEntry(null, 0);

    /**
     * Gets the value from cache, if any and non-expired, for the given key. Optionally it can be
     * instructed to refresh the value for the key in the background only if cache hit, in which case
     * the cached value is always returned, even if expired.
     * 
     * @param key cache key
     * @param valueSupplier (only applies when refresh=true) supplier for the value
     * @param ttl (only applies when refresh=true) time to cache the entry for - if null, then it will
     *        cache for the time specified in the config property
     *        <code>cache.graphqlquerycache.seconds</code>
     * @return optional of the cached value
     */

    public Optional<ExpirableCacheEntry> getEvenIfStale(final String key) {
        if (cannotCache()) {
            return Optional.empty();
        }

        final String cacheKey = generateKey(key);
        final ExpirableCacheEntry cacheEntry = (ExpirableCacheEntry) cache.getNoThrow(cacheKey, getPrimaryGroup());



        return Optional.ofNullable(cacheEntry);

    }



    public String generateKey(Object... objects) {
        long seed = 1125899906842597L;
        int modifier = 31;

        for (Object item : objects) {
            seed += seed * modifier + item.hashCode();
        }

        return "dotVelocityCache:" + seed;

    }

    public Optional<ExpirableCacheEntry> get(final String key) {
        Optional<ExpirableCacheEntry> entry = getEvenIfStale(key);
        return entry.isPresent() && !entry.get().isExpired() ? entry : Optional.empty();
    }


    /**
     * Puts into cache the value and expired time, based on the given ttl, for the given key
     * 
     * @param key the key
     * @param result the value
     * @param ttl time in seconds to consider the value as expired. If null, then it will cache the
     *        value for the time specified in the config property
     *        <code>cache.graphqlquerycache.seconds</code>
     */
    public void put(String key, Object resultIn, long ttl) {

        if (cannotCache() || key == null || resultIn == null || ttl == 0) {
            return;
        }

        if (!(resultIn instanceof Serializable)) {
            Logger.warn(this.getClass(),
                            () -> "Unable to cache key/value:" + key + "/" + resultIn + " as it is not serializable");
            return;
        }

        Serializable result = (Serializable) resultIn;


        final String cacheKey = generateKey(key);
        this.cache.put(cacheKey, new ExpirableCacheEntry(result, ttl), getPrimaryGroup());
    }

    /**
     * Puts into cache the value for the given key for the time specified in the config property
     * <code>cache.graphqlquerycache.seconds</code>
     * 
     * @param key the key
     * @param result the value
     */
    public void put(String key, Object result) {
        this.put(key, result, -1);
    }


    @Override
    public void clearCache() {
        cache.flushGroup(getPrimaryGroup());
    }

    @Override
    public String[] getGroups() {
        return new String[] {
                getPrimaryGroup()};
    }

    @Override
    public String getPrimaryGroup() {
        return "dotvelocitycache";
    }

    private boolean cannotCache() {
        return !LicenseManager.getInstance().isEnterprise();
    }

    public void remove(final String key) {
        final String cacheKey = generateKey(key);
        this.cache.remove(cacheKey, getPrimaryGroup());
    }




}
