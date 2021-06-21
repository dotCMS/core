package com.dotcms.publisher.pusher;

import com.dotcms.publisher.util.PusheableAsset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Use by {@link com.dotcms.publisher.pusher.PushPublisherConfig} to keep track of all the
 * assets added into a Bundle.
 */
class DependencySet {
    private Map<PusheableAsset, Set<String>> addedWithoutDependencies = new HashMap();
    private Map<PusheableAsset, Set<String>> addedWithDependencies  = new HashMap();

    /**
     * Add a asset
     * @param assetId
     * @param pusheableAsset
     */
    public synchronized void add(final String assetId, final PusheableAsset pusheableAsset){
        final Set<String> assets = getSet(pusheableAsset, addedWithoutDependencies);
        assets.add(assetId);
    }

    /**
     * Add a asset and also mark it to process its dependencies
     * @param assetId
     * @param pusheableAsset
     */
    public void addWithDependencies(final String assetId, final PusheableAsset pusheableAsset){
        final Set<String> assetsWithDependencies = getSet(pusheableAsset, addedWithDependencies);

        if (!assetsWithDependencies.contains(assetId)) {
            assetsWithDependencies.add(assetId);

            final Set<String> assetsWithoutDependencies =
                    getSet(pusheableAsset, addedWithoutDependencies);
            assetsWithoutDependencies.remove(assetId);
        }
     }

    private static Set<String> getSet(
            final PusheableAsset pusheableAsset,
            final Map<PusheableAsset, Set<String>> assetMap) {
        Set<String> assets = assetMap.get(pusheableAsset);

        if (assets == null) {
            assets = new HashSet<>();
            assetMap.put(pusheableAsset, assets);
        }
        return assets;
    }

    /**
     * Return true if the <code>assetId</code> is added with or without dependencies
     *
     * @param assetId
     * @param pusheableAsset
     * @return
     *
     * @see {@link DependencySet#add(String, PusheableAsset)}
     * @see {@link DependencySet#addWithDependencies(String, PusheableAsset)}
     */
    public boolean isAdded(final String assetId, final PusheableAsset pusheableAsset){
        final Set<String> assetsWithoutDependencies =
                getSet(pusheableAsset, addedWithoutDependencies);

        final Set<String> assetsWithDependencies =
                getSet(pusheableAsset, addedWithDependencies);

        return assetsWithoutDependencies.contains(assetId) ||
                assetsWithDependencies.contains(assetId);
    }

    /**
     * return true if <code>assetId</code> is added with dependencies before.
     * If it is not added or it is added but without dependencies then the method return false
     *
     * @param assetId
     * @param pusheableAsset
     * @return
     *
     * @see {@link DependencySet#add(String, PusheableAsset)}
     * @see {@link DependencySet#addWithDependencies(String, PusheableAsset)}
     */
    public boolean isDependenciesAdded(final String assetId, final PusheableAsset pusheableAsset){
        final Set<String> assetsWithDependencies =
                getSet(pusheableAsset, addedWithDependencies);

        return assetsWithDependencies.contains(assetId);
    }

    /**
     * Return all the asset's id add into the bundle, no matter if tey was added with or without
     * dependencies
     *
     * @param pusheableAsset
     * @return
     */
    private Set<String> getAll(final PusheableAsset pusheableAsset){
        final Set<String> assetsWithoutDependencies =
                getSet(pusheableAsset, addedWithDependencies);

        final Set<String> assetsWitDependencies =
                getSet(pusheableAsset, addedWithoutDependencies);

        return Stream.concat(assetsWithoutDependencies.stream(), assetsWitDependencies.stream())
                .collect(Collectors.toSet());
    }

    public Set<String> getContainers() {
        return getAll(PusheableAsset.CONTAINER);
    }

    public Set<String> getTemplates() {
        return getAll(PusheableAsset.TEMPLATE);
    }

    public Set<String> getContentlets() {
        return getAll(PusheableAsset.CONTENTLET);
    }

    public Set<String> getLinks() {
        return getAll(PusheableAsset.LINK);
    }

    public Set<String> getWorkflows() {
        return getAll(PusheableAsset.WORKFLOW);
    }

    public Set<String> getRules() {
        return getAll(PusheableAsset.RULE);
    }

    public Set<String> getRelationships() {
        return getAll(PusheableAsset.RELATIONSHIP);
    }

    public Set<String> getCategories() {
        return getAll(PusheableAsset.CATEGORY);
    }

    public Set<String> getFolders() {
        return getAll(PusheableAsset.FOLDER);
    }

    public Set<String> getStructures() {
        return getAll(PusheableAsset.CONTENT_TYPE);
    }

    public Set<String> getLanguages() {
        return getAll(PusheableAsset.LANGUAGE);
    }

    public Set<String> getHosts() {
        return getAll(PusheableAsset.SITE);
    }
}
