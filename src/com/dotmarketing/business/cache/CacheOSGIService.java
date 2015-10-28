package com.dotmarketing.business.cache;

import com.dotmarketing.business.cache.provider.CacheProvider;

/**
 * Service created in order to use as a bridge between OSGI plugins and the CacheProvides execution chain
 * @author Jonathan Gamba
 *         Date: 8/31/15
 */
public interface CacheOSGIService {

    /**
     * Adds a given CacheProvider class to the chain of CacheProviders to use for a given region, this method will instantiate and
     * initialize (init method) the given CacheProvider.
     *
     * @param cacheRegion
     * @param cacheProvider
     * @throws Exception
     */
    void addCacheProvider ( String cacheRegion, Class<CacheProvider> cacheProvider ) throws Exception;

    /**
     * Removes a given CacheProvider class from the chain of CacheProviders to use, this method before to delete
     * the record from the chain will invalidate (removeAll method) and shutdown (shutdown method) the given CacheProvider
     * if find in the current chain.
     *
     * @param cacheProvider
     */
    void removeCacheProvider ( Class<CacheProvider> cacheProvider );

}