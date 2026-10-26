package com.dotmarketing.startup.runonce;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

/**
 * Removes the legacy Containers portlet ({@code containers-legacy}) from every layout. The portlet
 * is no longer registered, so a layout that still lists it would keep a menu entry that cannot be
 * opened. The Angular Containers portlet ({@code containers}) is not touched.
 */
public class Task261023RemoveContainersLegacyPortletFromLayouts implements StartupTask {

    private static final String LEGACY_PORTLET_ID = "containers-legacy";

    /**
     * Tells whether any layout still lists the legacy Containers portlet. When the check itself
     * fails the error is logged and the task runs anyway: answering {@code false} would mark the
     * task as done and leave the rows in place for good, while running it surfaces a database
     * problem as a failed upgrade.
     *
     * @return {@code true} when at least one layout row references it, or when that cannot be told
     */
    @Override
    public boolean forceRun() {
        try {
            return new DotConnect()
                    .setSQL("select count(portlet_id) as count from cms_layouts_portlets where portlet_id = ?")
                    .addParam(LEGACY_PORTLET_ID)
                    .getInt("count") > 0;
        } catch (final Exception e) {
            Logger.error(this, "Could not check whether any layout still lists the '" + LEGACY_PORTLET_ID
                    + "' portlet, running the task anyway: " + e.getMessage(), e);
            return true;
        }
    }

    /**
     * Deletes the legacy Containers portlet from all layouts and clears the layout cache.
     *
     * @throws DotDataException    if the rows cannot be deleted
     * @throws DotRuntimeException if the layout cache cannot be cleared
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        new DotConnect()
                .setSQL("delete from cms_layouts_portlets where portlet_id = ?")
                .addParam(LEGACY_PORTLET_ID)
                .loadResult();

        CacheLocator.getLayoutCache().clearCache();
    }
}
