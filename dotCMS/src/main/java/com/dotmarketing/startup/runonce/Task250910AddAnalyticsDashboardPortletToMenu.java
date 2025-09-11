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
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotmarketing.util.PortletID.ANALYTICS_DASHBOARD;
import static com.dotmarketing.util.PortletID.SITES;

/**
 * Adds the custom 'Analytics Dashboard' portlet to all layouts which have 'Locales' portlet too, if
 * it does not already exist. If there is already a Analytics Dashboard portlet, that one will be
 * used instead.
 *
 * @author Jose Castro
 * @since Sep 10th, 2025
 */
public class Task250910AddAnalyticsDashboardPortletToMenu implements StartupTask {

    /**
     * Verifies if the custom {@code Analytics Dashboard} portlet must be added or not. It performs
     * the following data checks:
     * <ul>
     *     <li>The {@code Analytics Dashboard} portlet must be added in the same layout as either
     *     the {@code Marketing} or the {@code Settings} portlet.</li>
     *     <li>If neither of the expected Menu Groups can be found, the {@code Analytics Dashboard}
     *     portlet must be added manually then.</li>
     *     <li>If the {@code Analytics Dashboard} portlet is already present in any Menu Group, the
     *     UT can be skipped.</li>
     * </ul>
     *
     * @return If the UT must run, returns {@code true}.
     */
    @Override
    public boolean forceRun() {
        try {
            final List<Object> layoutIDs = this.getMenuGroupsForPortlet();
            if (UtilMethods.isNotSet(layoutIDs)) {
                Logger.warn(this, "The 'Analytics Dashboard' portlet could not be automatically added to any of the expected Menu Groups. " +
                        "Please add it manually");
                return false;
            }
            final Set<String> layoutsContainingAnalyticsDashboardPortlet =
                    layoutIDs.stream().map(layoutId -> new DotConnect()
                                    .setSQL("SELECT id FROM cms_layouts_portlets WHERE portlet_id = ?")
                                    .addParam(ANALYTICS_DASHBOARD.toString())
                                    .getString("id"))
                            .collect(Collectors.toSet());
            return layoutsContainingAnalyticsDashboardPortlet.stream().anyMatch(UtilMethods::isNotSet);
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when adding the 'Analytics Dashboard' portlet. " +
                    "Please add it manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    /**
     * Adds the custom {@code Analytics Dashboard} portlet to the appropriate Menu Group(s).
     *
     * @throws DotDataException An error occurred when adding the 'Analytics Dashboard' portlet.
     */
    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Adding the 'Analytics Dashboard' portlet to existing Menu Group(s)");
        final List<Object> layoutIDs = this.getMenuGroupsForPortlet();
        if (null != layoutIDs && !layoutIDs.isEmpty()) {
            for (final Object layoutID : layoutIDs) {
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
            }
            CacheLocator.getLayoutCache().clearCache();
            Logger.info(this, String.format("The 'Analytics Dashboard' portlet has been added to %d menu group(s) successfully!",
                    layoutIDs.size()));
        } else {
            Logger.error(this, "The 'Analytics Dashboard' portlet could not be added to any Menu Group. " +
                    "Please add it manually");
        }
    }

    /**
     * Returns all the Layout IDs; i.e., menu groups, for the {@code Marketing} group, or the one
     * containing the {@code Sites} portlet. That information will allow us to add the
     * {@code Analytics Dashboard} portlet in the expected location.
     *
     * @return List of Layout IDs where the new portlet will be added.
     *
     * @throws DotDataException An error occurred while querying the database.
     */
    private List<Object> getMenuGroupsForPortlet() throws DotDataException {
        final List<Object> layoutIds = new DotConnect().setSQL("SELECT id FROM cms_layout WHERE LOWER(layout_name) = 'marketing'")
                .loadObjectResults().stream().map(row -> row.get("id"))
                .collect(Collectors.toList());
        if (UtilMethods.isSet(layoutIds)) {
            return layoutIds;
        }
        return new DotConnect().setSQL("SELECT layout_id FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(SITES.toString())
                .loadObjectResults().stream().map(row -> row.get("layout_id"))
                .collect(Collectors.toList());
    }

}
