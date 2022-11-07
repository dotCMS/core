package com.dotmarketing.cache;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Table;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Provides the data structres used for caching Multi-Tree information in dotCMS.
 *
 * @author root
 */
public class MultiTreeCache implements Cachable {

    private final static String LIVE_GROUP = "pageMultiTreesLive";
    private final static String WORKING_GROUP = "pageMultiTreesWorking";
    private final static String CONTENTLET_REFERENCES_GROUP = "contentletReferences";
    private final static String[] GROUPS = {LIVE_GROUP, WORKING_GROUP, CONTENTLET_REFERENCES_GROUP};

    private final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    @Override
    public String getPrimaryGroup() {
        return LIVE_GROUP;
    }


    @SuppressWarnings("unchecked")
    public Optional<Table<String, String, Set<PersonalizedContentlet>>> getPageMultiTrees(final String pageIdentifier, final boolean live) {
        final String group = (live) ? LIVE_GROUP : WORKING_GROUP;

        try {
            return Optional.ofNullable((Table<String, String, Set<PersonalizedContentlet>>) cache.get(pageIdentifier, group));
        } catch (final DotCacheException e) {
            Logger.warn(this.getClass(), String.format("An error occurred when getting Multi-Tree cache data for page " +
                                                               "ID '%s': %s", pageIdentifier, e.getMessage()));
            return Optional.empty();
        }
    }

    public void putPageMultiTrees(final String pageIdentifier, final boolean live, final Table<String, String, Set<PersonalizedContentlet>> multiTrees) {
        final String group = (live) ? LIVE_GROUP : WORKING_GROUP;
        cache.put(pageIdentifier, multiTrees, group);
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

    /**
     * Returns the cached calculation on what Containers in the repository are referencing a specific Contentlet ID.
     *
     * @param contentletId The Contentlet ID to look for.
     *
     * @return The total number of Containers referencing it.
     */
    public Optional<Integer> getContentletReferenceCount(final String contentletId) {
        try {
            return Optional.ofNullable((Integer) this.cache.get(contentletId, CONTENTLET_REFERENCES_GROUP));
        } catch (final DotCacheException e) {
            Logger.warn(this.getClass(), String.format("An error occurred when getting reference count for contentlet" +
                                                               " ID '%s': %s", contentletId, e.getMessage()));
            return Optional.empty();
        }
    }

    /**
     * Sets the calculation on what Containers in the repository are referencing a specific Contentlet ID. This must
     * happen every time the Contentlet is added or removed from any Container.
     *
     * @param contentletId   The Contentlet ID being analyzed.
     * @param referenceCount The total number of Containers referencing it.
     */
    public void putContentletReferenceCount(final String contentletId, final int referenceCount) {
        this.cache.put(contentletId, referenceCount, CONTENTLET_REFERENCES_GROUP);
    }

    /**
     * Sets the calculated number of Containers in the repository that are referencing a specific Contentlet ID. This
     * must happen every time the Contentlet is added or removed from any Container.
     *
     * @param contentletId The Contentlet ID being analyzed.
     */
    public void removeContentletReferenceCount(final String contentletId) {
        this.cache.remove(contentletId, CONTENTLET_REFERENCES_GROUP);
    }

}
