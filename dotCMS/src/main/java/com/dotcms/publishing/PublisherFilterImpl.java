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
    private final boolean relationshipsSecondLevel;

    /**
     * Creates a filter with no key, and second-level relationship traversal disabled.
     *
     * @param dependencies  Whether dependencies should be followed.
     * @param relationships Whether direct relationships should be followed.
     */
    public PublisherFilterImpl(final boolean dependencies, final boolean relationships) {
        this(StringPool.BLANK, dependencies, relationships, false);
    }

    /**
     * Creates a filter with second-level relationship traversal disabled.
     *
     * @param key           The YAML filter file name this filter was built from.
     * @param dependencies  Whether dependencies should be followed.
     * @param relationships Whether direct relationships should be followed.
     */
    public PublisherFilterImpl(final String key, final boolean dependencies, final boolean relationships) {
        this(key, dependencies, relationships, false);
    }

    /**
     * Creates a filter with full control over second-level relationship traversal.
     *
     * @param key                      The YAML filter file name this filter was built from.
     * @param dependencies             Whether dependencies should be followed.
     * @param relationships            Whether direct relationships should be followed.
     * @param relationshipsSecondLevel Whether related content should also be followed one extra
     *                                 level beyond the direct relationships above.
     */
    public PublisherFilterImpl(final String key, final boolean dependencies, final boolean relationships,
            final boolean relationshipsSecondLevel) {
        this.key = key;
        this.dependencies = dependencies;
        this.relationships = relationships;
        this.relationshipsSecondLevel = relationshipsSecondLevel;
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

    @Override
    public boolean isRelationshipsSecondLevel() {
        return relationshipsSecondLevel;
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
                ", relationshipsSecondLevel=" + relationshipsSecondLevel +
                '}';
    }

}
