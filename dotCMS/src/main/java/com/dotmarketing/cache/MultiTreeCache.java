package com.dotmarketing.cache;


import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Table;
import java.util.*;
import static com.dotcms.util.CollectionsUtils.list;

/**
 * Provides the data structres used for caching Multi-Tree information in dotCMS.
 *
 * @author root
 */
public class MultiTreeCache implements Cachable {

    private static final String LIVE_GROUP = "pageMultiTreesLive";
    private static final String WORKING_GROUP = "pageMultiTreesWorking";
    private static final String VARIANTS_GROUP = "pageMultiTreesByVariant";
    private static final String CONTENTLET_REFERENCES_GROUP = "contentletReferences";
    private static final String[] GROUPS = {LIVE_GROUP, WORKING_GROUP, CONTENTLET_REFERENCES_GROUP, VARIANTS_GROUP};

    private final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    @Override
    public String getPrimaryGroup() {
        return LIVE_GROUP;
    }


    private String getCacheKey(final String pageIdentifier, final String variantName){
        return pageIdentifier + "_" + variantName;
    }

    @SuppressWarnings("unchecked")
    public Optional<Table<String, String, Set<PersonalizedContentlet>>> getPageMultiTrees(
            final String pageIdentifier, final String variantName, final boolean live) {
        try{
            final String group = (live) ? LIVE_GROUP : WORKING_GROUP;
            final String cacheKey = getCacheKey(pageIdentifier, variantName);

            final Table<String, String, Set<PersonalizedContentlet>> allPageMultiTress =
                    (Table<String, String, Set<PersonalizedContentlet>>) cache.get(cacheKey, group);

            return Optional.ofNullable(allPageMultiTress);
        } catch (final DotCacheException e) {
            Logger.warn(this.getClass(), String.format("An error occurred when getting Multi-Tree cache data for page ID '%s': %s",
                    pageIdentifier, e.getMessage()));
            return Optional.empty();
        }
    }


    public void putPageMultiTrees(final String pageIdentifier, final String variantName,
            final boolean live, final Table<String, String, Set<PersonalizedContentlet>> multiTreesToPut) {
        final String group = (live) ? LIVE_GROUP : WORKING_GROUP;
        final String cacheKey = getCacheKey(pageIdentifier, variantName);

        cache.put(cacheKey, multiTreesToPut, group);
        refreshPageVariantCache(pageIdentifier, variantName);
    }

    /**
     *
     * @param pageIdentifier
     * @param variantName
     */
    private void refreshPageVariantCache(final String pageIdentifier, final String variantName) {
        try {
            final String cacheKey = getCacheKey(pageIdentifier, variantName);
            final Set<String> variants = cache.get(cacheKey, VARIANTS_GROUP) != null ?
                    (Set<String>) cache.get(pageIdentifier, VARIANTS_GROUP) : new HashSet<>();

            variants.add(variantName);

            cache.remove(cacheKey, VARIANTS_GROUP);
            cache.put(pageIdentifier, variants, VARIANTS_GROUP);
        } catch (final DotCacheException e) {
            Logger.warn(this.getClass(), String.format("An error occurred when getting Multi-Tree cache data for page " +
                    "ID '%s': %s", pageIdentifier, e.getMessage()));
        }
    }


    public void removePageMultiTrees(final String pageIdentifier, final String variantName, final boolean live) {

        final String group = (live) ? LIVE_GROUP : WORKING_GROUP;
        final String cacheKey = getCacheKey(pageIdentifier, variantName);

        cache.remove(cacheKey, group);
    }

    public void removePageMultiTrees(final String pageIdentifier) {
        list(CONTENTLET_REFERENCES_GROUP, VARIANTS_GROUP).forEach(group -> cache.remove(pageIdentifier, group));

        getVariantsInCache(pageIdentifier).stream().forEach(variantName ->
                removeFromLiveAndWorking(pageIdentifier, variantName));

        cache.remove(pageIdentifier, VARIANTS_GROUP);
    }

    public void removePageMultiTrees(final String pageIdentifier, final String variantName) {
        list(CONTENTLET_REFERENCES_GROUP, VARIANTS_GROUP).forEach(group -> cache.remove(pageIdentifier, group));

        removeFromLiveAndWorking(pageIdentifier, variantName);

        cache.remove(pageIdentifier, VARIANTS_GROUP);
    }

    private void removeFromLiveAndWorking(String pageIdentifier, String variantName) {
        final String cacheKey = getCacheKey(pageIdentifier, variantName);
        cache.remove(cacheKey, WORKING_GROUP);
        cache.remove(cacheKey, LIVE_GROUP);
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
        try {
            Object variantsAsObject = cache.get(pageId, VARIANTS_GROUP);
            return variantsAsObject == null ? Collections.emptyList() : (Collection<String>) variantsAsObject;
        } catch (final DotCacheException e) {
            Logger.warn(this.getClass(), String.format("An error occurred when getting Variants from cache for page " +
                    "ID '%s': %s", pageId, e.getMessage()));
            return Collections.emptyList();
        }
    }
}
