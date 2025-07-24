package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

import java.util.Map;

public class CustomAttributeCacheImpl implements CustomAttributeCache {
    @Override
    public Map<String, String> get(final String eventTypeName) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

        try {
            return (Map<String, String>) cache.get(eventTypeName, getPrimaryGroup());
        } catch (DotCacheException e) {
            return null;
        }
    }

    @Override
    public void put(final String eventTypeName, final Map<String, String> attributesMatch) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.put(eventTypeName, attributesMatch, getPrimaryGroup());
    }

    @Override
    public String getPrimaryGroup() {
        return getClass().getSimpleName();
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public void clearCache() {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.flushGroup(getPrimaryGroup());
    }
}
