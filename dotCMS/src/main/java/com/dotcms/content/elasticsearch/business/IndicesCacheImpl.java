package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Logger;

public class IndicesCacheImpl implements IndicesCache {
    
    protected final DotCacheAdministrator cache;
    
    protected final String primaryGroup = "IndicesCache";
    protected final String[] groupNames = {primaryGroup};

    public IndicesCacheImpl() {
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
    
    public LegacyIndicesInfo get() {
        LegacyIndicesInfo info=null;
        try {
            info=(LegacyIndicesInfo)cache.get(primaryGroup+"info", primaryGroup);
        }
        catch(Exception ex) {
            Logger.warn(this, "can't get cache entry",ex);
        }
        return info;
    }
    
    public void put(LegacyIndicesInfo info) {
        cache.put(primaryGroup+"info", info, primaryGroup);
    }
}
