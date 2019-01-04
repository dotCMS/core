package com.dotmarketing.cache;

import java.util.Optional;
import java.util.Set;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Table;

public class MultiTreeCache implements Cachable {

    private final static String LIVE_GROUP = "pageMultiTreesLive";
    private final static String WORKING_GROUP = "pageMultiTreesWorking";
    
    private final static String[] GROUPS =  {LIVE_GROUP, WORKING_GROUP};
    
    
    private DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    @Override
    public String getPrimaryGroup() {
        return LIVE_GROUP;
    }


    @SuppressWarnings("unchecked")
    public Optional<Table<String, String, Set<String>>> getPageMultiTrees(final String pageIdentifier, boolean live) {
        String group = (live) ? LIVE_GROUP : WORKING_GROUP;

        try {
            return Optional.ofNullable((Table<String, String, Set<String>>) cache.get(pageIdentifier, group));
        } catch (DotCacheException e) {
            Logger.warn(this.getClass(), e.getMessage());
            return Optional.empty();
        }
    }

    public void putPageMultiTrees(final String pageIdentifier, boolean live, final Table<String, String, Set<String>> multiTrees) {
        String group = (live) ? LIVE_GROUP : WORKING_GROUP;
        cache.put(pageIdentifier, multiTrees, group);
    }


    public void removePageMultiTrees(final String pageIdentifier, boolean live) {
        String group = (live) ? LIVE_GROUP : WORKING_GROUP;
        removePageMultiTrees(pageIdentifier, group);
    }

    public void removePageMultiTrees(final String pageIdentifier) {
        for (final String group : getGroups()) {
            removePageMultiTrees(pageIdentifier, group);
        }
    }

    public void removePageMultiTrees(final String pageIdentifier, String group) {
        cache.remove(pageIdentifier, group);
    }

    @Override
    public String[] getGroups() {

        return GROUPS;
    }

    @Override
    public void clearCache() {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

        for (final String group : getGroups()) {
            cache.flushGroup(group);
        }

    }

}
