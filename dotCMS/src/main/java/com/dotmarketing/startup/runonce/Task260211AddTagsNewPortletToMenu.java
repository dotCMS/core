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
import static com.dotmarketing.util.PortletID.TAGS_LEGACY;

/**
 * Ensures the Angular 'Tags' portlet (id "tags", route /tags) is in the menu and the legacy
 * portlet (id "tags-legacy") is not. Removes tags-legacy from all layouts so it does not appear
 * in the sidebar; adds tags to the Content Types layout if missing (e.g. fresh installs).
 *
 * @author dotCMS
 * @since Feb 11th, 2026
 */
public class Task260211AddTagsNewPortletToMenu implements StartupTask {

    @Override
    public boolean forceRun() {
        try {
            final int legacyInLayouts = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(TAGS_LEGACY.toString())
                    .getInt("count");
            final int tagsInLayouts = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(TAGS.toString())
                    .getInt("count");
            final int tagsNewInLayouts = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = 'tags-new'")
                    .getInt("count");
            return legacyInLayouts > 0 || tagsInLayouts == 0 || tagsNewInLayouts > 0;
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when checking Tags menu state. " +
                    "Please add/remove portlets manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Ensuring Tags (Angular) is in the menu and Tags Legacy is hidden");

        // 0. Migrate any layout entries from old id "tags-new" to "tags" (portlet id was renamed)
        final int migrated = new DotConnect()
                .setSQL("UPDATE cms_layouts_portlets SET portlet_id = ? WHERE portlet_id = 'tags-new'")
                .addParam(TAGS.toString())
                .executeUpdate();
        if (migrated > 0) {
            Logger.info(this, "Migrated " + migrated + " layout entry(ies) from tags-new to tags");
        }

        // 1. Remove legacy from all layouts so it does not appear in the menu
        final int removed = new DotConnect()
                .setSQL("DELETE FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(TAGS_LEGACY.toString())
                .executeUpdate();
        if (removed > 0) {
            Logger.info(this, "Removed 'Tags Legacy' portlet from " + removed + " layout(s)");
        }

        // 2. If 'tags' (Angular) is still not in any layout after migration, add it to Content Types (or fallback)
        int tagsCount = new DotConnect()
                .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(TAGS.toString())
                .getInt("count");
        if (tagsCount == 0) {
            final String layoutID = getMenuGroupForPortlet();
            if (UtilMethods.isSet(layoutID)) {
                final int portletOrder = new DotConnect()
                        .setSQL("SELECT max(portlet_order) AS portlet_order FROM cms_layouts_portlets WHERE layout_id = ?")
                        .setMaxRows(1)
                        .addParam(layoutID)
                        .getInt("portlet_order");
                new DotConnect()
                        .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
                        .addParam(UUIDUtil.uuid())
                        .addParam(layoutID)
                        .addParam(TAGS.toString())
                        .addParam(portletOrder + 1)
                        .loadResult();
                Logger.info(this, "Added 'Tags' (Angular) portlet to the menu");
            } else {
                Logger.warn(this, "Could not find a layout to add the 'Tags' portlet. Please add it manually.");
            }
        }

        CacheLocator.getLayoutCache().clearCache();
        Logger.info(this, "Tags menu update completed successfully");
    }

    /**
     * Returns the Layout ID for the menu group where the Tags portlet should be added if missing
     * (typically "Content Types"), or a layout that contains the legacy portlet.
     *
     * @return The Layout ID, or null if not found.
     * @throws DotDataException An error occurred while querying the database.
     */
    private String getMenuGroupForPortlet() throws DotDataException {
        List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT id FROM cms_layout WHERE LOWER(layout_name) = 'content types'")
                .loadObjectResults();

        if (!results.isEmpty()) {
            String layoutId = results.get(0).getOrDefault("id", "").toString();
            if (UtilMethods.isSet(layoutId)) {
                return layoutId;
            }
        }

        results = new DotConnect()
                .setSQL("SELECT layout_id FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(TAGS_LEGACY.toString())
                .loadObjectResults();

        if (!results.isEmpty()) {
            return results.get(0).getOrDefault("layout_id", "").toString();
        }

        return null;
    }
}
