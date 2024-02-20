package com.dotcms.storage;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.liferay.util.StringPool;

import java.io.Serializable;

/**
 * This cache is used to store entries that we not found throughout the Storage Provider Chain. This
 * improves the overall performance as we can keep track of missing objects and avoid hitting every
 * provider in the chain.
 *
 * @author Jonathan Sanchez
 */
public class Chainable404StorageCache implements Cachable {

    private static final String CHAINABLE_404_GROUP = "Chainable_404_Storage_group";

    private final DotCacheAdministrator cache;

    public Chainable404StorageCache() {
        this(CacheLocator.getCacheAdministrator());
    }

    public Chainable404StorageCache(final DotCacheAdministrator cache) {
        this.cache = cache;
    }

    // This is the flag we use to mark an objects previous request and do not exist as 404
    public static final Serializable NOT_FOUND_404 = "CHAINABLE_404_STORAGE_NOT_FOUND";

    @Override
    public String getPrimaryGroup() {

        return CHAINABLE_404_GROUP;
    }

    @Override
    public String[] getGroups() {

        return new String[] {CHAINABLE_404_GROUP};
    }

    @Override
    public void clearCache() {
        cache.flushGroup(CHAINABLE_404_GROUP);
    }

    /**
     * Mark the bucket + path as 404
     * @param bucket
     * @param path
     */
    public void put404(final String bucket, final String path) {
        if (bucket == null || null == path) {
            return;
        }
        cache.put(key(bucket, path), NOT_FOUND_404, this.getPrimaryGroup());
    }

    /**
     * Check if the bucket + path is 404
     * @param bucket
     * @param path
     * @return
     */
    public boolean is404(final String bucket, final String path) {

        if (bucket == null || null == path) {
            return false;
        }

        final Object cacheResult = cache.getNoThrow(key(bucket, path), this.getPrimaryGroup());

        return NOT_FOUND_404.equals(cacheResult);
    }

    /**
     * Remove the 404 from the cache
     * @param bucket
     * @param path
     */
    public void remove(final String bucket, final String path) {

        if (bucket != null && null != path) {

            cache.remove(key(bucket, path), this.getPrimaryGroup());
        }
    }

    String key(final String bucket, final String path) {
        return bucket + StringPool.UNDERLINE + path;
    }

}
