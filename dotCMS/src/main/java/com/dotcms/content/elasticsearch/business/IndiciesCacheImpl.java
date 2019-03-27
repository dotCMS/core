package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Logger;

public class IndiciesCacheImpl implements IndiciesCache {
    
    protected final DotCacheAdministrator cache;
    
    protected final String primaryGroup = "IndiciesCache";
    protected final String[] groupNames = {primaryGroup};

    public IndiciesCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }
    
    public String getPrimaryGroup() {
        return primaryGroup;
    }
    
    public String[] getGroups() {
        return groupNames;
    }
    
    public void clearCache() {
        cache.flushGroup(primaryGroup);
    }
    
    public IndiciesInfo get() {
        IndiciesInfo info=null;
        try {
            info=(IndiciesInfo)cache.get(primaryGroup+"info", primaryGroup);
        }
        catch(Exception ex) {
            Logger.warn(this, "can't get cache entry",ex);
        }
        return info;
    }
    
    public void put(IndiciesInfo info) {
        cache.put(primaryGroup+"info", info, primaryGroup);
    }
}
