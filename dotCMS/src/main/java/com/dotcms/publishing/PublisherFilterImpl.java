package com.dotcms.publishing;


import java.util.HashSet;
import java.util.Set;

/**
 * This class is use to load the values of the filters of a FilterDescriptor that is going to be use
 * to create a bundle.
 * Each method check for each filter and check that the asset that is going to be added to the bundle
 * is accepted.
 */
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

    public String toString(){
        return "PublisherFilter {" +
                " excludeClassesSet = " + this.excludeClassesSet.toString() +
                " , excludeDependencyClassesSet = " + this.excludeDependencyClassesSet.toString() +
                " , excludeQueryIds = " + this.excludeQueryAssetIdSet.toString() +
                " , excludeDependencyQueryIds = " + this.excludeDependencyQueryAssetIdSet.toString() +
                " , relationships = " + this.relationships +
                " , dependencies = " + this.dependencies +
                "}";


    }
}
