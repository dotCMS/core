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
import static com.dotmarketing.util.PortletID.PLUGINS;
import static com.dotmarketing.util.PortletID.PLUGINS_LEGACY;

/**
 * Replaces the legacy {@code plugins-legacy} (formerly {@code dynamic-plugins}) portlet in the
 * admin menu with the Angular {@code plugins} portlet. The legacy portlet is removed from all
 * layouts so it no longer appears in the sidebar; it remains registered in {@code portlet.xml}
 * and can be added back manually if needed.
 *
 * @author Humberto Morera
 * @since Mar 20th, 2026
 */
public class Task260320AddPluginsPortletToMenu implements StartupTask {

    @Override
    public boolean forceRun() {
        try {
            final int legacyCount = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ? OR portlet_id = ?")
                    .addParam(PLUGINS_LEGACY.toString())
                    .addParam(DYNAMIC_PLUGINS.toString())
                    .getInt("count");
            if (legacyCount > 0) {
                return true;
            }
            final int pluginsCount = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(PLUGINS.toString())
                    .getInt("count");
            return pluginsCount == 0;
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when checking the 'Plugins' portlet. " +
                    "Please verify manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Replacing legacy plugins portlet with Angular 'plugins' portlet in the admin menu");

        // 1. Normalize: rename dynamic-plugins -> plugins-legacy in DB to match portlet.xml
        final int dynamicPluginsCount = new DotConnect()
                .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(DYNAMIC_PLUGINS.toString())
                .getInt("count");
        if (dynamicPluginsCount > 0) {
            new DotConnect()
                    .setSQL("UPDATE cms_layouts_portlets SET portlet_id = ? WHERE portlet_id = ?")
                    .addParam(PLUGINS_LEGACY.toString())
                    .addParam(DYNAMIC_PLUGINS.toString())
                    .loadResult();
            Logger.info(this, "Renamed 'dynamic-plugins' to 'plugins-legacy' in cms_layouts_portlets");
        }

        // 2. Find layout and position of plugins-legacy before removing it
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT layout_id, portlet_order FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(PLUGINS_LEGACY.toString())
                .loadObjectResults();

        if (results.isEmpty() || !UtilMethods.isSet(results.get(0).getOrDefault("layout_id", "").toString())) {
            Logger.error(this, "Could not find 'plugins-legacy' in any layout. " +
                    "The 'plugins' portlet cannot be added automatically. Please add it manually.");
            return;
        }

        final String layoutId = results.get(0).get("layout_id").toString();
        final int legacyOrder = Integer.parseInt(results.get(0).getOrDefault("portlet_order", "0").toString());

        // 3. Remove plugins-legacy from all layouts (no longer visible in sidebar)
        new DotConnect()
                .setSQL("DELETE FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(PLUGINS_LEGACY.toString())
                .loadResult();
        Logger.info(this, "Removed 'plugins-legacy' from all layouts");

        // 4. Insert plugins at the same position plugins-legacy occupied
        final int pluginsCount = new DotConnect()
                .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(PLUGINS.toString())
                .getInt("count");
        if (pluginsCount == 0) {
            new DotConnect()
                    .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
                    .addParam(UUIDUtil.uuid())
                    .addParam(layoutId)
                    .addParam(PLUGINS.toString())
                    .addParam(legacyOrder)
                    .loadResult();
            Logger.info(this, "Added 'plugins' portlet at position " + legacyOrder + " in layout: " + layoutId);
        }

        CacheLocator.getLayoutCache().clearCache();
        Logger.info(this, "The 'plugins' portlet has been added to the admin menu successfully!");
    }

}
