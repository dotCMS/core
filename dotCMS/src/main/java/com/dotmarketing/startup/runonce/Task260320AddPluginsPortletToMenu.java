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

/**
 * Adds the Angular {@code plugins} portlet to the admin menu alongside the existing
 * {@code dynamic-plugins} portlet. The {@code dynamic-plugins} row is intentionally left
 * untouched in {@code cms_layouts_portlets} so that a version rollback automatically restores
 * the JSP-based portlet — the old {@code portlet.xml} already registers {@code dynamic-plugins}
 * and no DB repair is needed.
 *
 * @author Humberto Morera
 * @since Mar 20th, 2026
 */
public class Task260320AddPluginsPortletToMenu implements StartupTask {

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
        Logger.info(this, "Adding Angular 'plugins' portlet to the admin menu alongside the legacy portlet");

        // Find layout and position of dynamic-plugins.
        // The row is intentionally left untouched so that a version rollback finds 'dynamic-plugins'
        // in cms_layouts_portlets and the old portlet.xml renders the JSP portlet without any DB repair.
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT layout_id, portlet_order FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(DYNAMIC_PLUGINS.toString())
                .loadObjectResults();

        if (results.isEmpty() || !UtilMethods.isSet(results.get(0).getOrDefault("layout_id", "").toString())) {
            Logger.error(this, "Could not find 'dynamic-plugins' in any layout. " +
                    "The 'plugins' portlet cannot be added automatically. Please add it manually.");
            return;
        }

        final String layoutId = results.get(0).get("layout_id").toString();
        final int legacyOrder = Integer.parseInt(results.get(0).getOrDefault("portlet_order", "0").toString());

        // Insert plugins at the same position the legacy portlet occupies.
        // The legacy row is left untouched — see comment above.
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
