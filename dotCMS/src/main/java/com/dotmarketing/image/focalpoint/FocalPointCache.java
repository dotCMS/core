package com.dotmarketing.image.focalpoint;

import java.util.Optional;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * FocalPoint cache
 */
public class FocalPointCache implements Cachable {

    private DotCacheAdministrator cache;

    private static String primaryGroup = "FocalPointCache";

    // region's name for the cache
    private static String[] groupNames = { primaryGroup };

    /**
     * Default constructor
     */
    public FocalPointCache() {
        cache = CacheLocator.getCacheAdministrator();
    }

    /**
     *
     * @param inode
     * @param fieldVar
     * @return
     */
    private String key(final String inode, final String fieldVar) {

        return primaryGroup + inode + fieldVar;
    }

    /**
     * add cache entry
     * @param inode
     * @param fieldVar
     * @param focalPoint
     * @return
     */
    public Optional<FocalPoint> add(final String inode, final String fieldVar, final FocalPoint focalPoint) {

        if (UtilMethods.isSet(inode) && UtilMethods.isSet(fieldVar)) {
            cache.put(key(inode, fieldVar), focalPoint, primaryGroup);
        }

        return Optional.ofNullable(focalPoint);
    }

    /**
     * Clear all cache
     */
    public void clearCache() {
        cache.flushGroup(primaryGroup);
    }

    /**
     * Remove entry
     * @param inode
     * @param fieldVar
     */
    public void remove(final String inode, final String fieldVar) {

        cache.remove(key(inode, fieldVar), primaryGroup);
    }

    /**
     * List all available groups
     * @return
     */
    public String[] getGroups() {
        return groupNames;
    }

    /**
     * Cache Primary group
     * @return
     */
    public String getPrimaryGroup() {
        return primaryGroup;
    }

    /**
     * Cache getter
     * @param inode
     * @param fieldVar
     * @return
     */
    public Optional<FocalPoint> get(final String inode, final String fieldVar) {
        try {
            return Optional.ofNullable ((FocalPoint) cache.get(key(inode, fieldVar), primaryGroup));
        } catch (DotCacheException e) {
            Logger.debug(this, "Cache not able to be gotten", e);
        }
        return Optional.empty();
    }
}
