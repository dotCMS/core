package com.dotcms.publishing;


import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import java.util.HashSet;
import java.util.Set;

public class PublisherFilterImpl implements PublisherFilter{

    private final Set<String> excludeClassesSet = new HashSet<>();
    private final Set<String>excludeDependencyClassesSet = new HashSet<>();
    private final Set<String>excludeQueryAssetIdSet = new HashSet<>();
    private final Set<String>excludeDependencyQueryAssetIdSet = new HashSet<>();
    private final boolean dependencies;
    private final boolean relationships;

    public PublisherFilterImpl(final boolean dependencies, final boolean relationships) {
        this.dependencies = dependencies;
        this.relationships = relationships;
    }

    @Override
    public boolean isDependencies() {
        return dependencies;
    }

    @Override
    public boolean isRelationships() {
        return relationships;
    }

    public void addTypeToExcludeDependencyClassesSet(final String type) {
        this.excludeDependencyClassesSet.add(type.toLowerCase());
    }

    public void addTypeToExcludeClassesSet(final String type) {
        this.excludeClassesSet.add(type.toLowerCase());
    }

    public void addContentletIdToExcludeQueryAssetIdSet(final String contentletId) {
        this.excludeQueryAssetIdSet.add(contentletId);
    }

    public void addContentletIdToExcludeDependencyQueryAssetIdSet(final String contentletId) {
        this.excludeDependencyQueryAssetIdSet.add(contentletId);
    }

    @Override
    public boolean acceptExcludeClasses(final String assetType) {
        return !this.excludeClassesSet.contains(assetType.toLowerCase());
    }

    @Override
    public boolean acceptExcludeQuery(final String contentletId) {
        return !this.excludeQueryAssetIdSet.contains(contentletId);
    }

    @Override
    public boolean acceptExcludeDependencyQuery(final String contentletId) {
        return !this.excludeDependencyQueryAssetIdSet.contains(contentletId);
    }

    @Override
    public boolean acceptExcludeDependencyClasses(final String pusheableAssetType) {
        return !this.excludeDependencyClassesSet.contains(pusheableAssetType.toLowerCase());
    }
}
