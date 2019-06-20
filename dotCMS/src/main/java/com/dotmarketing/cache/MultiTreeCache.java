package com.dotmarketing.cache;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Table;

public class MultiTreeCache implements Cachable {

    private final static String LIVE_GROUP = "pageMultiTreesLive";
    private final static String WORKING_GROUP = "pageMultiTreesWorking";
    private final static String[] GROUPS = {LIVE_GROUP, WORKING_GROUP};


    private DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    @Override
    public String getPrimaryGroup() {
        return LIVE_GROUP;
    }


    @SuppressWarnings("unchecked")
    public Optional<Table<String, String, Set<PersonalizedContentlet>>> getPageMultiTrees(final String pageIdentifier, final boolean live) {
        final String group = (live) ? LIVE_GROUP : WORKING_GROUP;

        try {
            return Optional.ofNullable((Table<String, String, Set<PersonalizedContentlet>>) cache.get(pageIdentifier, group));
        } catch (DotCacheException e) {
            Logger.warn(this.getClass(), e.getMessage());
            return Optional.empty();
        }
    }

    public void putPageMultiTrees(final String pageIdentifier, final boolean live, final Table<String, String, Set<PersonalizedContentlet>> multiTrees) {
        final String group = (live) ? LIVE_GROUP : WORKING_GROUP;
        cache.put(pageIdentifier, multiTrees, group);
    }


    public void removePageMultiTrees(final String pageIdentifier, final boolean live) {
        final String group = (live) ? LIVE_GROUP : WORKING_GROUP;
        removePageMultiTrees(pageIdentifier, group);
    }

    public void removePageMultiTrees(final String pageIdentifier) {
        Arrays.asList(getGroups()).forEach(group -> removePageMultiTrees(pageIdentifier, group));
    }

    public void removePageMultiTrees(final String pageIdentifier, final String group) {
        cache.remove(pageIdentifier, group);
    }

    @Override
    public String[] getGroups() {

        return GROUPS;
    }

    @Override
    public void clearCache() {
        Arrays.asList(getGroups()).forEach(group ->  cache.flushGroup(group));
    }

}
