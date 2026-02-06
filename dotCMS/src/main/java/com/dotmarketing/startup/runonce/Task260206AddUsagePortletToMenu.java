package com.dotmarketing.startup.runonce;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;

import java.util.List;
import java.util.Map;

import static com.dotmarketing.util.PortletID.SITES;
import static com.dotmarketing.util.PortletID.USAGE;

/**
 * Adds the custom 'Usage' portlet to the main menu, if it hasn't already been added yet.
 *
 * @author Jose Castro
 * @since Feb 6th, 2026
 */
public class Task260206AddUsagePortletToMenu implements StartupTask {

    @Override
    public boolean forceRun() {
        try {
            final String layoutID = this.getMenuGroupForPortlet();
            if (UtilMethods.isNotSet(layoutID)) {
                Logger.warn(this, "The 'Usage' portlet could not be automatically added to any of the expected Menu Groups. " +
                        "Please add it manually");
                return false;
            }
            final int count = new DotConnect()
                                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                                    .addParam(USAGE.toString())
                                    .getInt("count");
            return count == 0;
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when adding the 'Usage' portlet. " +
                    "Please add it manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    /**
     * Adds the custom {@code Usage} portlet to the appropriate Menu Group.
     *
     * @throws DotDataException An error occurred when adding the 'Usage' portlet.
     */
    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Adding the 'Usage' portlet to existing Menu Group(s)");
        final String layoutID = this.getMenuGroupForPortlet();
        if (null != layoutID && !layoutID.isEmpty()) {
            final boolean isLayoutMissingUsagePortlet = 0 == new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE layout_id = ? AND portlet_id = ?")
                    .addParam(layoutID)
                    .addParam(USAGE.toString())
                    .getInt("count");
            if (isLayoutMissingUsagePortlet) {
                final int portletOrder = new DotConnect()
                        .setSQL("SELECT max(portlet_order) AS portlet_order FROM cms_layouts_portlets WHERE layout_id = ?")
                        .setMaxRows(1)
                        .addParam(layoutID)
                        .getInt("portlet_order");
                new DotConnect()
                        .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
                        .addParam(UUIDUtil.uuid())
                        .addParam(layoutID)
                        .addParam(USAGE.toString())
                        .addParam(portletOrder + 1)
                        .loadResult();
            }
            CacheLocator.getLayoutCache().clearCache();
            Logger.info(this, "The 'Usage' portlet has been added to the main menu successfully!");
        } else {
            Logger.error(this, "The 'Usage' portlet could not be added to any Menu Group. " +
                    "Please add it manually");
        }
    }

    /**
     * Returns the Layout ID; i.e., menu group, for the Menu Groups that are meant to hold the
     * {@code Usage} portlet, depending on whether they're present or not. The order
     * of priority is the following:
     * <ol>
     *     <li>Look for the {@code System} group.</li>
     *     <li>If not present, look for the {@code Marketing} group.</li>
     *     <li>If not present, fall back to the group containing the {@code Sites} portlet.</li>
     * </ol>
     *
     * @return The Layout ID that the new portlet will be added to.
     *
     * @throws DotDataException An error occurred while querying the database.
     */
    private String getMenuGroupForPortlet() throws DotDataException {
        // Try System layout first
        List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT id FROM cms_layout WHERE LOWER(layout_name) = 'system'")
                .loadObjectResults();

        if (!results.isEmpty()) {
            String layoutId = results.get(0).getOrDefault("id", "").toString();
            if (UtilMethods.isSet(layoutId)) {
                return layoutId;
            }
        }

        // Try Marketing layout second
        results = new DotConnect()
                .setSQL("SELECT id FROM cms_layout WHERE LOWER(layout_name) = 'marketing'")
                .loadObjectResults();

        if (!results.isEmpty()) {
            String layoutId = results.get(0).getOrDefault("id", "").toString();
            if (UtilMethods.isSet(layoutId)) {
                return layoutId;
            }
        }

        // Fall back to layout containing SITES portlet
        results = new DotConnect()
                .setSQL("SELECT layout_id FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(SITES.toString())
                .loadObjectResults();

        if (!results.isEmpty()) {
            return results.get(0).getOrDefault("layout_id", "").toString();
        }

        Logger.warn(this, "No suitable layout found for Usage portlet");
        return null;
    }

}
