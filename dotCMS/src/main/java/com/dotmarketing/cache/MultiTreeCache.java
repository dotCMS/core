package com.dotmarketing.cache;


import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Table;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides the data structres used for caching Multi-Tree information in dotCMS.
 *
 * @author root
 */
public class MultiTreeCache implements Cachable {

    private static final String LIVE_GROUP = "pageMultiTreesLive";
    private static final String WORKING_GROUP = "pageMultiTreesWorking";
    private static final String CONTENTLET_REFERENCES_GROUP = "contentletReferences";
    private static final String[] GROUPS = {LIVE_GROUP, WORKING_GROUP, CONTENTLET_REFERENCES_GROUP};

    private final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    @Override
    public String getPrimaryGroup() {
        return LIVE_GROUP;
    }


    @SuppressWarnings("unchecked")
    public Optional<Table<String, String, Set<PersonalizedContentlet>>> getPageMultiTrees(
            final String pageIdentifier, final String variantName, final boolean live) {
        final String group = (live) ? LIVE_GROUP : WORKING_GROUP;

        final Optional<Map<String, Table<String, String, Set<PersonalizedContentlet>>>> allPageMultiTress =
                getMultiTreeMap(pageIdentifier, group);

        return allPageMultiTress.isPresent() ? Optional.ofNullable(allPageMultiTress.get().get(variantName))
                : Optional.empty();
    }

    private Optional<Map<String, Table<String, String, Set<PersonalizedContentlet>>>> getMultiTreeMap(
            final String pageIdentifier, final String group) {

        try {
            return Optional.ofNullable((Map<String, Table<String, String, Set<PersonalizedContentlet>>>) cache.get(
                            pageIdentifier, group));
        } catch (final DotCacheException e) {
            Logger.warn(this.getClass(), String.format("An error occurred when getting Multi-Tree cache data for page " +
                    "ID '%s': %s", pageIdentifier, e.getMessage()));
            return Optional.empty();
        }
    }

    public void putPageMultiTrees(final String pageIdentifier, final String variantName,
            final boolean live, final Table<String, String, Set<PersonalizedContentlet>> multiTreesToPut) {
        final String group = (live) ? LIVE_GROUP : WORKING_GROUP;
        final Map<String, Table<String, String, Set<PersonalizedContentlet>>> multiTreeMap =
                getMultiTreeMap(pageIdentifier, group).orElseGet(() -> new HashMap<>());

        multiTreeMap.put(variantName, multiTreesToPut);
        cache.put(pageIdentifier, multiTreeMap, group);
    }


    public void removePageMultiTrees(final String pageIdentifier, final String variantName, final boolean live) {

        final String group = (live) ? LIVE_GROUP : WORKING_GROUP;
        removePageMultiTrees(pageIdentifier, variantName, group);
    }

    public void removePageMultiTrees(final String pageIdentifier) {
        Arrays.asList(getGroups()).forEach(group -> cache.remove(pageIdentifier, group));
    }

    public void removePageMultiTrees(final String pageIdentifier, final String variantName) {
        Arrays.asList(getGroups()).forEach(group -> removePageMultiTrees(pageIdentifier, variantName, group));
    }

    public void removePageMultiTrees(final String pageIdentifier, final String variantName, final String group) {

        final Map<String, Table<String, String, Set<PersonalizedContentlet>>> multiTreeMap =
                getMultiTreeMap(pageIdentifier, group).orElseGet(() -> new HashMap<>());

        multiTreeMap.remove(variantName);

        if (multiTreeMap.isEmpty()) {
            cache.remove(pageIdentifier, group);
        } else {
            cache.put(pageIdentifier, multiTreeMap, group);
        }
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

    public Collection<String> getVariantsInCache(final String pageId) {
        return Arrays.stream(getGroups())
                .flatMap(group -> getMultiTreeMap(pageId, group).stream())
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toSet());
    }
}
