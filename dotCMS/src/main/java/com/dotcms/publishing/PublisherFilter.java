package com.dotcms.publishing;

/**
 * This class is to store the values of the diff filters of the filter selected to apply in the bundle.
 * Adds each value to their respective Set or Boolean using the add method or the constructor.
 * The accept methods are to check if the param is in the respective Set, if is returns false and it
 * if is not returns true, so the asset can be added to the bundle.
 */
public interface PublisherFilter {

    /**
     * Check if the asset needs to be excluded from the bundle.
     * This because the excludeClasses contains the type of the asset.
     *
     * @param type assetType that is gonna be added to bundle
     * @return boolean if the asset Type is in the set
     */
    boolean acceptExcludeClasses(final String type);

    /**
     * Check if the contentlet needs to be excluded from the bundle.
     * This because the result of the query (excludeQuery) contains the id of the contentlet.
     *
     * @param contentletId id of the contentlet that is gonna be added to the bundle
     * @return boolean if the contentletId is in the set
     */
    boolean acceptExcludeQuery(final String contentletId);

    /**
     * Check if the contentlet needs to be excluded from the bundle.
     * This because the result of the query (excludeDependencyQuery) contains the id of the contentlet.
     *
     * @param contentletId id of the contentlet that is gonna be added to the bundle
     * @return boolean if the contentletId is in the set
     */
    boolean acceptExcludeDependencyQuery(final String contentletId);

    /**
     * Check if the pusheableAsset needs to be excluded from the bundle.
     * This because the excludeDependencyClasses contains the type of the asset.
     *
     * @param pusheableAssetType asset that is gonna be added to bundle
     * @return boolean if the asset Type is in the set
     */
    boolean acceptExcludeDependencyClasses(final String pusheableAssetType);

    /**
     *
     * @return boolean value of the relationships param.
     */
    boolean isRelationships();

    /**
     *
     * @return boolean value of the dependencies param.
     */
    boolean isDependencies();
}
