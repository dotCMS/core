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
import javax.enterprise.context.ApplicationScoped;
import java.util.stream.Stream;

/**
 * Encapsulates cache flush business logic and post-flush side effects
 * (permission reference reset and PushPublishing filter reload).
 * Shared by both the flush-region and flush-all REST endpoints.
 *
 * @author hassandotcms
 */
@ApplicationScoped
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
     * Validates the region name against known {@code CacheIndex} values.
     *
     * @param regionName the cache region name (must match a {@code CacheIndex} value)
     * @throws BadRequestException if the region name is not recognized
     */
    public void flushRegion(final String regionName) {

        if (!isValidRegion(regionName)) {
            throw new BadRequestException("Unknown cache region: " + regionName);
        }

        CacheLocator.getCache(regionName).clearCache();

        performPostFlushActions(regionName);
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

    private boolean isValidRegion(final String regionName) {

        final Object[] caches = CacheLocator.getCacheIndexes();
        return Stream.of(caches)
                .anyMatch(idx -> idx.toString().equals(regionName));
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

        DotInitializer.class.cast(APILocator.getPublisherAPI()).init();
    }

}
