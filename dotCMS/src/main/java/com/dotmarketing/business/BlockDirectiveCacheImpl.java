package com.dotmarketing.business;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.util.Map;

public class BlockDirectiveCacheImpl extends BlockDirectiveCache {



    private boolean canCache;
    private final DotCacheAdministrator cache;

    private final String group = "BlockDirectiveCache";


    // regions name for the cache
    private final String[] groupNames = {
            group};

    public BlockDirectiveCacheImpl() {
        this(CacheLocator.getCacheAdministrator(), (LicenseUtil.getLevel() >= LicenseLevel.COMMUNITY.level));

    }

    @VisibleForTesting
    public BlockDirectiveCacheImpl(DotCacheAdministrator cache, boolean canCache) {
        this.cache = cache;
        this.canCache = canCache;
    }

    public void clearCache() {
        // clear the cache
        cache.flushGroup(group);
    }

    @Override
    public void add(String key, Map<String, Serializable> value, int ttlInSeconds) {
        if (key == null || value == null || !canCache) {
            return;
        }
        BlockDirectiveCacheObject cto = new BlockDirectiveCacheObject(value, ttlInSeconds);
        cache.put(key, cto, group);

    }
    

    
    public void remove(String key) {
        cache.remove(key, group);
    }

    public String[] getGroups() {
        return groupNames;
    }

    public String getPrimaryGroup() {
        return group;
    }

    private static final Map<String, Serializable> EMPTY_MAP = Map.of();

    @Override
    public Map<String, Serializable> get(String key) {
        if (!canCache) {
            return EMPTY_MAP;
        }

        Object o = cache.getNoThrow(key, group);
        if (o == null) {
            return EMPTY_MAP;
        }
        if (o instanceof Map) {
            return (Map<String, Serializable>) o;
        }

        if (!(o instanceof BlockDirectiveCacheObject)) {
            return EMPTY_MAP;
        }

        BlockDirectiveCacheObject bdco = (BlockDirectiveCacheObject) o;

        return bdco.getMap();

    }
    



}
