package com.dotmarketing.startup.runonce;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

/**
 * Takes the legacy ES Search ({@code es-search-legacy}) and legacy Query Tool
 * ({@code query-tool-legacy}) portlets out of every layout.
 *
 * <p>Both portlets rendered a JSP that no longer exists, and neither is declared in
 * {@code portlet.xml} anymore, so a layout that still lists one points at a tool that cannot be
 * opened. The Angular ES Search and Query Tool portlets replace them. Neither legacy portlet is in
 * a default layout, so this only affects an instance where an admin added one by hand.
 *
 * <p>The task only deletes the layout entries; it does not touch the Angular portlets. A version
 * rollback finds the previous release's {@code portlet.xml} with both legacy portlets declared
 * again, and an admin can add them back to a layout from Roles &amp; Tools.
 *
 * @since October 28th, 2026
 */
public class Task261028RemoveSearchLegacyPortletsFromLayouts implements StartupTask {

    private static final String ES_SEARCH_LEGACY = "es-search-legacy";
    private static final String QUERY_TOOL_LEGACY = "query-tool-legacy";

    /**
     * Runs only while a layout still lists one of the two legacy portlets, so an instance that
     * never had them skips the task.
     *
     * @return {@code true} if at least one layout entry for a legacy portlet exists
     */
    @Override
    public boolean forceRun() {
        try {
            final int legacyCount = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id IN (?, ?)")
                    .addParam(ES_SEARCH_LEGACY)
                    .addParam(QUERY_TOOL_LEGACY)
                    .getInt("count");
            return legacyCount > 0;
        } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when checking for the legacy search "
                    + "portlets in the layouts. Please verify manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    /**
     * Deletes the layout entries of both legacy portlets and clears the layout cache so the menu
     * stops listing them. Running it again finds nothing to delete.
     *
     * @throws DotDataException if the entries cannot be deleted
     */
    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Removing the legacy ES Search and Query Tool portlets from the layouts");

        new DotConnect()
                .setSQL("DELETE FROM cms_layouts_portlets WHERE portlet_id IN (?, ?)")
                .addParam(ES_SEARCH_LEGACY)
                .addParam(QUERY_TOOL_LEGACY)
                .loadResult();

        CacheLocator.getLayoutCache().clearCache();
    }
}
