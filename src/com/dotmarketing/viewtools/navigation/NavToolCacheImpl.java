package com.dotmarketing.viewtools.navigation;

import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

public class NavToolCacheImpl implements NavToolCache {
    
    private static final String GROUP="NavTool";
    private DotCacheAdministrator cache;
    
    public NavToolCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }
    
    protected static String buildKey(String hostid, String path) {
        return hostid+":"+path;
    }
    
    @Override
    public List<NavResult> getNav(String hostid, String path) {
        try {
            return (List<NavResult>) cache.get(buildKey(hostid,path),GROUP);
        }
        catch(DotCacheException ex) {
            return null;
        }
    }
    
    @Override
    public void putNav(String hostid, String path, List<NavResult> items) {
        cache.put(buildKey(hostid,path), items, GROUP);
    }
    
    @Override
    public void removeNav(String hostid, String path) {
        String key=buildKey(hostid,path);
        cache.remove(key, GROUP);
    }
    
    @Override
    public void removeNavAndChildren(String hostid, String path) {
        String key=buildKey(hostid,path);
        cache.remove(key, GROUP);
        for(String k : cache.getKeys(GROUP))
            if(k.startsWith(key))
                cache.remove(k, GROUP);
    }

    @Override
    public String getPrimaryGroup() {
        return GROUP;
    }

    @Override
    public String[] getGroups() {
        return new String[]{GROUP};
    }

    @Override
    public void clearCache() {
        cache.flushGroup(GROUP);
    }
    
}
