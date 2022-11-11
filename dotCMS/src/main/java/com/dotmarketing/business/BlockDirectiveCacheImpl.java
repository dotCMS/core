package com.dotmarketing.business;

import java.io.Serializable;
import java.util.Map;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;

public class BlockDirectiveCacheImpl extends BlockDirectiveCache {

    @Override
    public void add(String key, Map<String, Serializable> value, int ttl) {
        if (key == null || value == null) {
            return;
        }
        BlockDirectiveCacheObject cto = new BlockDirectiveCacheObject(value, ttl);
        cache.put(key, cto, group);

    }



    private boolean canCache;
    private DotCacheAdministrator cache;

    private String group = "BlockDirectiveCache";
    private String secondaryGroup = "BlockDirectiveHTMLPageCache";

    // regions name for the cache
    private String[] groupNames = {
            group,
            secondaryGroup};

    public BlockDirectiveCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
        // delete everything on startup
        canCache = LicenseUtil.getLevel() >= LicenseLevel.COMMUNITY.level;


    }

    /*
     * (non-Javadoc)
     * 
     * @see com.dotmarketing.business.PermissionCache#clearCache()
     */
    public void clearCache() {
        // clear the cache
        cache.flushGroup(group);
        cache.flushGroup(secondaryGroup);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
     */
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
    public Map<String, Serializable> get(String key, int ttl) {
        if (!canCache) {
            return EMPTY_MAP;
        }

        BlockDirectiveCacheObject cto = (BlockDirectiveCacheObject) cache.getNoThrow(key, group);
        if (cto == null) {
            return EMPTY_MAP;
        }
        if (cto.getCreated() + (ttl * 1000) > System.currentTimeMillis()) {
            return cto.getMap();
        }

        cache.removeLocalOnly(key, group, false);
        return EMPTY_MAP;
    }



}
