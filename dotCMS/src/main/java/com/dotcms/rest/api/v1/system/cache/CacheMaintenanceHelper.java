package com.dotcms.rest.api.v1.system.cache;

import com.dotcms.config.DotInitializer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.google.common.annotations.VisibleForTesting;

import com.dotcms.rest.exception.BadRequestException;
import java.util.stream.Stream;

/**
 * Encapsulates cache flush business logic and post-flush side effects
 * (permission reference reset and PushPublishing filter reload).
 * Shared by both the flush-region and flush-all REST endpoints.
 *
 * @author hassandotcms
 */
public class CacheMaintenanceHelper {

    private final PermissionAPI permissionAPI;

    public CacheMaintenanceHelper() {
        this(APILocator.getPermissionAPI());
    }

    @VisibleForTesting
    public CacheMaintenanceHelper(final PermissionAPI permissionAPI) {
        this.permissionAPI = permissionAPI;
    }

    /**
     * Flushes a specific cache region by name, then performs post-flush side effects.
     * Region name lookup is case-insensitive; the canonical name from {@code CacheIndex} is used.
     *
     * @param regionName the cache region name (case-insensitive)
     * @return the canonical region name that was flushed
     * @throws BadRequestException if the region name is not recognized
     */
    public String flushRegion(final String regionName) {

        final String canonical = resolveRegionName(regionName);
        if (canonical == null) {
            throw new BadRequestException("Unknown cache region: " + regionName);
        }

        CacheLocator.getCache(canonical).clearCache();

        performPostFlushActions(canonical);

        return canonical;
    }

    /**
     * Flushes all caches via {@link MaintenanceUtil#flushCache()},
     * then performs post-flush side effects including PushPublishing reload.
     */
    public void flushAllCaches() {

        MaintenanceUtil.flushCache();

        try {
            permissionAPI.resetAllPermissionReferences();
        } catch (DotDataException e) {
            Logger.error(this, "Error resetting permission references after flushing all caches", e);
        }

        reloadPublishingFilters();
    }

    /**
     * Resolves a region name case-insensitively to its canonical {@code CacheIndex} value.
     *
     * @return the canonical region name, or {@code null} if not found
     */
    private String resolveRegionName(final String regionName) {

        final Object[] caches = CacheLocator.getCacheIndexes();
        return Stream.of(caches)
                .map(Object::toString)
                .filter(name -> name.equalsIgnoreCase(regionName))
                .findFirst()
                .orElse(null);
    }

    private void performPostFlushActions(final String regionName) {

        try {
            permissionAPI.resetAllPermissionReferences();
        } catch (DotDataException e) {
            Logger.error(this, "Error resetting permission references after flushing " + regionName, e);
        }

        if ("system".equalsIgnoreCase(regionName)) {
            reloadPublishingFilters();
        }
    }

    private void reloadPublishingFilters() {

        try {
            DotInitializer.class.cast(APILocator.getPublisherAPI()).init();
        } catch (Exception e) {
            Logger.error(this, "Error reloading PushPublishing filters", e);
        }
    }

}
