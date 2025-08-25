package com.dotcms.analytics.attributes;


import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

/**
 * Default {@link CustomAttributeCache} implementation backed by {@link DotCacheAdministrator}.
 */
@ApplicationScoped
public class CustomAttributeCacheImpl implements CustomAttributeCache {
    /**
     * Fetches the mapping for the given event type from the cache.
     * Returns {@code null} on cache miss or cache-related errors.
     */
    @Override
    public Map<String, String> get(final String eventTypeName) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

        try {
            return (Map<String, String>) cache.get(eventTypeName, getPrimaryGroup());
        } catch (DotCacheException e) {
            return null;
        }
    }

    /**
     * Puts the mapping for the provided event type into the cache.
     */
    @Override
    public void put(final String eventTypeName, final Map<String, String> attributesMatch) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.put(eventTypeName, attributesMatch, getPrimaryGroup());
    }

    /** {@inheritDoc} */
    @Override
    public String getPrimaryGroup() {
        return getClass().getSimpleName();
    }

    /** {@inheritDoc} */
    @Override
    public String[] getGroups() {
        return new String[0];
    }

    /** {@inheritDoc} */
    @Override
    public void clearCache() {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.flushGroup(getPrimaryGroup());
    }
}
