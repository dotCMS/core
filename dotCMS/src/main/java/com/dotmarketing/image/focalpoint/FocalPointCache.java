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

    public FocalPointCache() {

        cache = CacheLocator.getCacheAdministrator();
    }

    private String key(final String inode, final String fieldVar) {

        return primaryGroup + inode + fieldVar;
    }



    public FocalPoint add(final String inode, final String fieldVar, final FocalPoint focalPoint) {

        if (UtilMethods.isSet(inode) && UtilMethods.isSet(fieldVar)) {
            cache.put(key(inode, fieldVar), focalPoint, primaryGroup);
        }

        return focalPoint;
    }


    public void clearCache() {
        cache.flushGroup(primaryGroup);
    }


    public void remove(final String inode, final String fieldVar) {

        cache.remove(key(inode, fieldVar), primaryGroup);
    }



    public String[] getGroups() {
        return groupNames;
    }

    public String getPrimaryGroup() {
        return primaryGroup;
    }


    public Optional<FocalPoint> get(final String inode, final String fieldVar) {

        FocalPoint retVal = null;
        try {
            retVal = (FocalPoint) cache.get(key(inode, fieldVar), primaryGroup);
        } catch (DotCacheException e) {
            Logger.debug(this, "Cache not able to be gotten", e);

        }
        return Optional.ofNullable(retVal);
    }
}
