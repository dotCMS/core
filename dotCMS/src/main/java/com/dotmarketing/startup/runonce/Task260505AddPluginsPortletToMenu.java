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

import static com.dotmarketing.util.PortletID.DYNAMIC_PLUGINS;
import static com.dotmarketing.util.PortletID.MAINTENANCE;
import static com.dotmarketing.util.PortletID.PLUGINS;
import static com.dotmarketing.util.PortletID.PLUGINS_LEGACY;

/**
 * Recovery task for issue #35428. Ensures the new Angular {@code plugins} portlet is present
 * in a sensible default layout on instances that were left in a broken state by the
 * pre-{@code empty_20260331} starter. Those instances have a {@code dynamic-plugins} row in
 * {@code cms_layouts_portlets} (seeded by the old starter) but no {@code plugins} row, because
 * {@link Task260320AddPluginsPortletToMenu} was marked complete on first install without ever
 * executing its body (the {@code firstTimeStart} skip in
 * {@link com.dotmarketing.startup.StartupTasksExecutor#executeSchemaUpgrades}).
 *
 * <p>The layout is resolved through a fallback chain so the task is robust to whatever combination
 * of legacy rows and renamed layouts the customer's database is in:
 *
 * <ol>
 *   <li>Layout containing {@code dynamic-plugins} (legacy seed).</li>
 *   <li>Layout containing {@code plugins-legacy} (post-rename intermediate state).</li>
 *   <li>Layout named {@code system} (case-insensitive) — stable anchor by name.</li>
 *   <li>Layout containing the {@code maintenance} portlet — last-resort System anchor by content.</li>
 * </ol>
 *
 * <p>Inserts only the new {@code plugins} portlet. Leaves any existing {@code dynamic-plugins} and
 * {@code plugins-legacy} rows untouched, preserving the rollback-safety convention asserted by
 * {@code Task260320AddPluginsPortletToMenuTest#dynamicPluginsIsPreservedForRollbackSafety}.
 *
 * @author hassandotcms
 * @since May 5th, 2026
 */
public class Task260505AddPluginsPortletToMenu implements StartupTask {

    @Override
    public boolean forceRun() {
        try {
            final int pluginsCount = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(PLUGINS.toString())
                    .getInt("count");
            return pluginsCount == 0;
        } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when checking the 'Plugins' portlet. " +
                    "Please verify manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Ensuring the Angular 'plugins' portlet is present in the admin menu");

        final String layoutId = resolveTargetLayout();
        if (UtilMethods.isNotSet(layoutId)) {
            Logger.error(this, "No suitable layout found to host the 'plugins' portlet. " +
                    "Please add it manually via Roles & Tools.");
            return;
        }

        final boolean alreadyInLayout = new DotConnect()
                .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE layout_id = ? AND portlet_id = ?")
                .addParam(layoutId)
                .addParam(PLUGINS.toString())
                .getInt("count") > 0;
        if (alreadyInLayout) {
            Logger.info(this, "The 'plugins' portlet is already present in layout " + layoutId + "; nothing to do.");
            return;
        }

        final int nextOrder = new DotConnect()
                .setSQL("SELECT MAX(portlet_order) AS portlet_order FROM cms_layouts_portlets WHERE layout_id = ?")
                .addParam(layoutId)
                .getInt("portlet_order") + 1;

        new DotConnect()
                .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
                .addParam(UUIDUtil.uuid())
                .addParam(layoutId)
                .addParam(PLUGINS.toString())
                .addParam(nextOrder)
                .loadResult();
        Logger.info(this, "Added 'plugins' portlet at position " + nextOrder + " in layout: " + layoutId);

        CacheLocator.getLayoutCache().clearCache();
    }

    /**
     * Picks the layout that should host the new {@code plugins} portlet, walking the fallback chain
     * documented on the class. Returns {@code null} when no anchor can be found, which signals the
     * caller to log and exit cleanly without inserting anything.
     */
    private String resolveTargetLayout() throws DotDataException {
        // 1. Layout containing 'dynamic-plugins' (legacy seed).
        String layoutId = findLayoutContainingPortlet(DYNAMIC_PLUGINS.toString());
        if (UtilMethods.isSet(layoutId)) {
            return layoutId;
        }

        // 2. Layout containing 'plugins-legacy' (post-rename intermediate state).
        layoutId = findLayoutContainingPortlet(PLUGINS_LEGACY.toString());
        if (UtilMethods.isSet(layoutId)) {
            return layoutId;
        }

        // 3. Layout named 'system' (case-insensitive).
        final List<Map<String, Object>> systemByName = new DotConnect()
                .setSQL("SELECT id FROM cms_layout WHERE LOWER(layout_name) = 'system'")
                .loadObjectResults();
        if (!systemByName.isEmpty()) {
            final String id = systemByName.get(0).getOrDefault("id", "").toString();
            if (UtilMethods.isSet(id)) {
                return id;
            }
        }

        // 4. Layout containing the 'maintenance' portlet.
        layoutId = findLayoutContainingPortlet(MAINTENANCE.toString());
        if (UtilMethods.isSet(layoutId)) {
            return layoutId;
        }

        return null;
    }

    private String findLayoutContainingPortlet(final String portletId) throws DotDataException {
        // ORDER BY layout_id keeps the choice deterministic in the rare case the same portlet ID
        // appears in more than one layout (cms_layouts_portlets UNIQUE is on (layout_id, portlet_id),
        // not portlet_id alone).
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT layout_id FROM cms_layouts_portlets WHERE portlet_id = ? ORDER BY layout_id")
                .setMaxRows(1)
                .addParam(portletId)
                .loadObjectResults();
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0).getOrDefault("layout_id", "").toString();
    }

}
