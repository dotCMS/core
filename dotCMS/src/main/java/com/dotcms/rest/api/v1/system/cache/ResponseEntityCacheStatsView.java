package com.dotcms.rest.api.v1.system.cache;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for the cache statistics endpoint.
 *
 * @author hassandotcms
 */
public class ResponseEntityCacheStatsView extends ResponseEntityView<CacheStatsView> {

    public ResponseEntityCacheStatsView(final CacheStatsView entity) {
        super(entity);
    }
}
