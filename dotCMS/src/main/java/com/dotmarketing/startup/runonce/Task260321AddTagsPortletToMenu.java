package com.dotmarketing.startup.runonce;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;

import java.util.List;
import java.util.Map;

import static com.dotmarketing.util.PortletID.CATEGORIES;
import static com.dotmarketing.util.PortletID.TAGS;
import static com.dotmarketing.util.PortletID.TAGS_LEGACY;

/**
 * Migrates the Tags portlet entries in the admin menu:
 * <ol>
 *     <li>Renames any {@code tags-new} portlet_id to {@code tags} (portlet was renamed).</li>
 *     <li>Removes {@code tags-legacy} from all layouts.</li>
 *     <li>If {@code tags} is still absent from all layouts, adds it to the "Content Types"
 *     layout, falling back to the layout that holds the {@code categories} portlet.</li>
 *     <li>Clears the layout cache.</li>
 * </ol>
 * <p>
 * Task id {@code 260321} is distinct from {@link Task260320AddPluginsPortletToMenu} ({@code 260320});
 * both digits after "Task" map to {@code db_version} rows and must be unique.
 *
 * @author Humberto Morera
 * @since Mar 21st, 2026
 */
public class Task260321AddTagsPortletToMenu implements StartupTask {

    private static final String TAGS_NEW_PORTLET_ID = "tags-new";

    @Override
    public boolean forceRun() {
        try {
            final int tagsLegacyCount = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(TAGS_LEGACY.toString())
                    .getInt("count");
            if (tagsLegacyCount > 0) {
                return true;
            }
            final int tagsNewCount = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(TAGS_NEW_PORTLET_ID)
                    .getInt("count");
            if (tagsNewCount > 0) {
                return true;
            }
            final int tagsCount = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(TAGS.toString())
                    .getInt("count");
            return tagsCount == 0;
        } catch (final DotDataException | DotRuntimeException e) {
            Logger.error(this, String.format("An error occurred when checking the 'Tags' portlet. " +
                    "Please verify manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    /**
     * @throws DotDataException An error occurred while updating layouts.
     */
    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Migrating Tags portlet menu entries");

        // 1. Migrate tags-new -> tags
        final int tagsNewCount = new DotConnect()
                .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(TAGS_NEW_PORTLET_ID)
                .getInt("count");
        if (tagsNewCount > 0) {
            new DotConnect()
                    .setSQL("UPDATE cms_layouts_portlets SET portlet_id = ? WHERE portlet_id = ?")
                    .addParam(TAGS.toString())
                    .addParam(TAGS_NEW_PORTLET_ID)
                    .loadResult();
            Logger.info(this, "Renamed 'tags-new' to 'tags' in cms_layouts_portlets");
        }

        // 2. Remove tags-legacy from all layouts
        final int tagsLegacyRowCount = new DotConnect()
                .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(TAGS_LEGACY.toString())
                .getInt("count");
        new DotConnect()
                .setSQL("DELETE FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(TAGS_LEGACY.toString())
                .loadResult();
        if (tagsLegacyRowCount > 0) {
            Logger.info(this, String.format("Removed %d 'tags-legacy' row(s) from layouts", tagsLegacyRowCount));
        }

        // 3. Add tags if still absent from all layouts
        final int tagsCount = new DotConnect()
                .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(TAGS.toString())
                .getInt("count");
        if (tagsCount == 0) {
            final String layoutId = getLayoutIdForTags();
            if (UtilMethods.isSet(layoutId)) {
                final int portletOrder = new DotConnect()
                        .setSQL("SELECT COALESCE(MAX(portlet_order), 0) AS portlet_order FROM cms_layouts_portlets WHERE layout_id = ?")
                        .setMaxRows(1)
                        .addParam(layoutId)
                        .getInt("portlet_order");
                new DotConnect()
                        .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
                        .addParam(UUIDUtil.uuid())
                        .addParam(layoutId)
                        .addParam(TAGS.toString())
                        .addParam(portletOrder + 1)
                        .loadResult();
                Logger.info(this, "Added 'tags' portlet to layout: " + layoutId);
            } else {
                Logger.error(this, "Could not find a suitable layout for the 'tags' portlet. Please add it manually.");
            }
        }

        CacheLocator.getLayoutCache().clearCache();
        Logger.info(this, "Tags portlet menu migration completed successfully");
    }

    /**
     * Returns the layout ID where the {@code tags} portlet should be added. Tries the
     * "Content Types" layout first, then falls back to the layout that holds the
     * {@code categories} portlet (same menu group).
     */
    private String getLayoutIdForTags() throws DotDataException {
        final List<Map<String, Object>> contentTypesLayout = new DotConnect()
                .setSQL("SELECT id FROM cms_layout WHERE LOWER(layout_name) = 'content types'")
                .loadObjectResults();
        if (!contentTypesLayout.isEmpty()) {
            final String layoutId = contentTypesLayout.get(0).getOrDefault("id", "").toString();
            if (UtilMethods.isSet(layoutId)) {
                return layoutId;
            }
        }
        // Fallback: layout containing 'categories', which lives in the same group as tags
        final List<Map<String, Object>> categoriesLayout = new DotConnect()
                .setSQL("SELECT layout_id FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(CATEGORIES.toString())
                .loadObjectResults();
        if (!categoriesLayout.isEmpty()) {
            return categoriesLayout.get(0).getOrDefault("layout_id", "").toString();
        }
        Logger.warn(this, "No suitable layout found for 'tags' portlet");
        return null;
    }

}
