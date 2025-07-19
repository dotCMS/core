package com.dotcms.rest.api.v1.system.cache;

import com.dotcms.rest.ResponseEntityView;

/**
 * ResponseEntityView for generic cache objects in cache management endpoints.
 * Used for cache objects of any type retrieved from cache providers.
 */
public class ResponseEntityCacheObjectView extends ResponseEntityView<Object> {
    
    public ResponseEntityCacheObjectView(Object entity) {
        super(entity);
    }
}