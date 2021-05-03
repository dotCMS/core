package com.dotcms.publisher.util.dependencies;

import com.dotcms.publisher.util.PusheableAsset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class DependencySet {
    private Map<PusheableAsset, Set<String>> addedWithoutDependencies = new HashMap();
    private Map<PusheableAsset, Set<String>> addedWithDependencies  = new HashMap();

    public void add(final String assetId, final PusheableAsset pusheableAsset){
        final Set<String> assets = getSet(pusheableAsset, addedWithoutDependencies);
        assets.add(assetId);
    }

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

    public boolean isAdded(final String assetId, final PusheableAsset pusheableAsset){
        final Set<String> assetsWithoutDependencies =
                getSet(pusheableAsset, addedWithoutDependencies);

        return assetsWithoutDependencies.contains(assetId);
    }

    public boolean isDependenciesAdded(final String assetId, final PusheableAsset pusheableAsset){
        final Set<String> assetsWithoutDependencies =
                getSet(pusheableAsset, addedWithDependencies);

        return assetsWithoutDependencies.contains(assetId);
    }

    public Set<String> getAll(final PusheableAsset pusheableAsset){
        final Set<String> assetsWithoutDependencies =
                getSet(pusheableAsset, addedWithDependencies);

        final Set<String> assetsWitDependencies =
                getSet(pusheableAsset, addedWithDependencies);

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
}
