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

import static com.dotmarketing.util.PortletID.TAGS;
import static com.dotmarketing.util.PortletID.TAGS_NEW;

/**
 * Adds the Angular 'Tags' portlet (tags-new) to the same menu group as the legacy Tags portlet,
 * so it appears in the /api/v1/menu response and in the admin sidebar.
 *
 * @author dotCMS
 * @since Feb 11th, 2026
 */
public class Task260211AddTagsNewPortletToMenu implements StartupTask {

    @Override
    public boolean forceRun() {
        try {
            final String layoutID = this.getMenuGroupForPortlet();
            if (UtilMethods.isNotSet(layoutID)) {
                Logger.warn(this, "The 'Tags' (Angular) portlet could not be automatically added: " +
                        "no layout containing the legacy Tags portlet was found. Please add it manually.");
                return false;
            }
            final int count = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(TAGS_NEW.toString())
                    .getInt("count");
            return count == 0;
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when adding the 'Tags' (Angular) portlet. " +
                    "Please add it manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Adding the 'Tags' (Angular) portlet to existing Menu Group(s)");
        final String layoutID = this.getMenuGroupForPortlet();
        if (null != layoutID && !layoutID.isEmpty()) {
            final boolean isLayoutMissingTagsNewPortlet = 0 == new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE layout_id = ? AND portlet_id = ?")
                    .addParam(layoutID)
                    .addParam(TAGS_NEW.toString())
                    .getInt("count");
            if (isLayoutMissingTagsNewPortlet) {
                final int portletOrder = new DotConnect()
                        .setSQL("SELECT max(portlet_order) AS portlet_order FROM cms_layouts_portlets WHERE layout_id = ?")
                        .setMaxRows(1)
                        .addParam(layoutID)
                        .getInt("portlet_order");
                new DotConnect()
                        .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
                        .addParam(UUIDUtil.uuid())
                        .addParam(layoutID)
                        .addParam(TAGS_NEW.toString())
                        .addParam(portletOrder + 1)
                        .loadResult();
            }
            CacheLocator.getLayoutCache().clearCache();
            Logger.info(this, "The 'Tags' (Angular) portlet has been added to the main menu successfully!");
        } else {
            Logger.error(this, "The 'Tags' (Angular) portlet could not be added to any Menu Group. " +
                    "Please add it manually");
        }
    }

    /**
     * Returns the Layout ID of the menu group that contains the legacy Tags portlet
     * (typically "Content Types"), so the new Angular Tags portlet is added to the same group.
     *
     * @return The Layout ID that the new portlet will be added to, or null if not found.
     * @throws DotDataException An error occurred while querying the database.
     */
    private String getMenuGroupForPortlet() throws DotDataException {
        // Prefer layout named "Content Types" (where Tags usually lives)
        List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT id FROM cms_layout WHERE LOWER(layout_name) = 'content types'")
                .loadObjectResults();

        if (!results.isEmpty()) {
            String layoutId = results.get(0).getOrDefault("id", "").toString();
            if (UtilMethods.isSet(layoutId)) {
                return layoutId;
            }
        }

        // Fall back to layout containing the legacy TAGS portlet
        results = new DotConnect()
                .setSQL("SELECT layout_id FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(TAGS.toString())
                .loadObjectResults();

        if (!results.isEmpty()) {
            return results.get(0).getOrDefault("layout_id", "").toString();
        }

        Logger.warn(this, "No suitable layout found for Tags (Angular) portlet");
        return null;
    }
}
