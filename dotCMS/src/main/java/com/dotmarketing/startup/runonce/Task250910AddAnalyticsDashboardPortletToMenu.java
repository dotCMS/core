package com.dotmarketing.startup.runonce;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;

import static com.dotmarketing.util.PortletID.ANALYTICS_DASHBOARD;
import static com.dotmarketing.util.PortletID.SITES;

/**
 * Adds the custom 'Analytics Dashboard' portlet to the main menu, if it hasn't already been added
 * yet.
 *
 * @author Jose Castro
 * @since Sep 10th, 2025
 */
public class Task250910AddAnalyticsDashboardPortletToMenu implements StartupTask {

    @Override
    public boolean forceRun() {
        try {
            final String layoutID = this.getMenuGroupForPortlet();
            if (UtilMethods.isNotSet(layoutID)) {
                Logger.warn(this, "The 'Analytics Dashboard' portlet could not be automatically added to any of the expected Menu Groups. " +
                        "Please add it manually");
                return false;
            }
            final String analyticsDashboardPortlet = new DotConnect()
                                    .setSQL("SELECT id FROM cms_layouts_portlets WHERE portlet_id = ?")
                                    .addParam(ANALYTICS_DASHBOARD.toString())
                                    .getString("id");
            return UtilMethods.isNotSet(analyticsDashboardPortlet);
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when adding the 'Analytics Dashboard' portlet. " +
                    "Please add it manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    /**
     * Adds the custom {@code Analytics Dashboard} portlet to the appropriate Menu Group.
     *
     * @throws DotDataException An error occurred when adding the 'Analytics Dashboard' portlet.
     */
    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Adding the 'Analytics Dashboard' portlet to existing Menu Group(s)");
        final String layoutID = this.getMenuGroupForPortlet();
        if (null != layoutID && !layoutID.isEmpty()) {
            final boolean isLayoutMissingLangVarPortlet = 0 == new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE layout_id = ? AND portlet_id = ?")
                    .addParam(layoutID)
                    .addParam(ANALYTICS_DASHBOARD.toString())
                    .getInt("count");
            if (isLayoutMissingLangVarPortlet) {
                final int portletOrder = new DotConnect()
                        .setSQL("SELECT max(portlet_order) AS portlet_order FROM cms_layouts_portlets WHERE layout_id = ?")
                        .setMaxRows(1)
                        .addParam(layoutID)
                        .getInt("portlet_order");
                new DotConnect()
                        .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
                        .addParam(UUIDUtil.uuid())
                        .addParam(layoutID)
                        .addParam(ANALYTICS_DASHBOARD.toString())
                        .addParam(portletOrder + 1)
                        .loadResult();
            }
            CacheLocator.getLayoutCache().clearCache();
            Logger.info(this, "The 'Analytics Dashboard' portlet has been added to the main menu successfully!");
        } else {
            Logger.error(this, "The 'Analytics Dashboard' portlet could not be added to any Menu Group. " +
                    "Please add it manually");
        }
    }

    /**
     * Returns the Layout ID; i.e., menu group, for the Menu Groups that are meant to hold the
     * {@code Analytics Dashboard} portlet, depending on whether they're present or not. The order
     * of priority is the following:
     * <ol>
     *     <li>Look for the {@code Site Manager} group, for the Empty Starter only.</li>
     *     <li>If not present, look for the {@code Marketing} group.</li>
     *     <li>If not present, fall back to the group containing the {@code Sites} portlet.</li>
     * </ol>
     *
     * @return The Layout ID that the new portlet will be added to.
     *
     * @throws DotDataException An error occurred while querying the database.
     */
    private String getMenuGroupForPortlet() throws DotDataException {
        String layoutId = new DotConnect().setSQL("SELECT id FROM cms_layout WHERE LOWER(layout_name) = 'site manager'")
                .loadObjectResults().get(0).getOrDefault("id", "").toString();
        if (UtilMethods.isSet(layoutId)) {
            return layoutId;
        }
        layoutId = new DotConnect().setSQL("SELECT id FROM cms_layout WHERE LOWER(layout_name) = 'marketing'")
                .loadObjectResults().get(0).getOrDefault("id", "").toString();
        if (UtilMethods.isSet(layoutId)) {
            return layoutId;
        }
        return new DotConnect().setSQL("SELECT layout_id FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(SITES.toString())
                .loadObjectResults().get(0).getOrDefault("layout_id", "").toString();
    }

}
