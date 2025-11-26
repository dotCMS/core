package com.dotcms.publishing;

import com.liferay.util.StringPool;

import java.util.HashSet;
import java.util.Set;

/**
 * This class is use to load the values of the filters of a FilterDescriptor that is going to be use
 * to create a bundle.
 * Each method check for each filter and check that the asset that is going to be added to the bundle
 * is accepted.
 */
public class PublisherFilterImpl implements PublisherFilter{

    private final String key;
    private final Set<String> excludeClassesSet = new HashSet<>();
    private final Set<String> excludeDependencyClassesSet = new HashSet<>();
    private final Set<String> excludeQueryAssetIdSet = new HashSet<>();
    private final Set<String> excludeDependencyQueryAssetIdSet = new HashSet<>();
    private final boolean dependencies;
    private final boolean relationships;

    public PublisherFilterImpl(final boolean dependencies, final boolean relationships) {
        this(StringPool.BLANK, dependencies, relationships);
    }

    public PublisherFilterImpl(final String key, final boolean dependencies, final boolean relationships) {
        this.key = key;
        this.dependencies = dependencies;
        this.relationships = relationships;
    }

    @Override
    public String key() {
        return this.key;
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
    public boolean doesExcludeClassesContainsType(final String assetType) {
        return this.excludeClassesSet.contains(assetType.toLowerCase());
    }

    @Override
    public boolean doesExcludeQueryContainsContentletId(final String contentletId) {
        return this.excludeQueryAssetIdSet.contains(contentletId);
    }

    @Override
    public boolean doesExcludeDependencyQueryContainsContentletId(final String contentletId) {
        return this.excludeDependencyQueryAssetIdSet.contains(contentletId);
    }

    @Override
    public boolean doesExcludeDependencyClassesContainsType(final String pusheableAssetType) {
        return this.excludeDependencyClassesSet.contains(pusheableAssetType.toLowerCase());
    }

    @Override
    public String toString() {
        return "PublisherFilterImpl{" +
                "key='" + key + '\'' +
                ", excludeClassesSet=" + excludeClassesSet +
                ", excludeDependencyClassesSet=" + excludeDependencyClassesSet +
                ", excludeQueryAssetIdSet=" + excludeQueryAssetIdSet +
                ", excludeDependencyQueryAssetIdSet=" + excludeDependencyQueryAssetIdSet +
                ", dependencies=" + dependencies +
                ", relationships=" + relationships +
                '}';
    }

}
