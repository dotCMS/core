package com.dotcms.rest.api.v1.system.cache;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.business.cache.provider.CacheProvider;

/**
 * ResponseEntityView for CacheProvider objects in cache management endpoints.
 */
public class ResponseEntityCacheProviderView extends ResponseEntityView<CacheProvider> {
    
    public ResponseEntityCacheProviderView(CacheProvider entity) {
        super(entity);
    }
}