package com.dotmarketing.business.cache;

import com.dotmarketing.business.cache.provider.CacheProvider;

/**
 * @author Jonathan Gamba
 *         Date: 8/31/15
 */
public interface CacheOSGIService {

    void addCacheProvider ( Class<CacheProvider> cacheProvider ) throws Exception;

    void removeCacheProvider ( Class<CacheProvider> cacheProvider );

}