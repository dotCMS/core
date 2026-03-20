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

import static com.dotmarketing.util.PortletID.DYNAMIC_PLUGINS;
import static com.dotmarketing.util.PortletID.PLUGINS;
import static com.dotmarketing.util.PortletID.PLUGINS_LEGACY;

/**
 * Adds the Angular {@code plugins} portlet to the admin menu immediately after the legacy
 * {@code plugins-legacy} (formerly {@code dynamic-plugins}) portlet in the same menu group.
 * Also renames any {@code dynamic-plugins} entry in {@code cms_layouts_portlets} to
 * {@code plugins-legacy} to keep the DB consistent with {@code portlet.xml}.
 *
 * @author Humberto Morera
 * @since Mar 20th, 2026
 */
public class Task260320AddPluginsPortletToMenu implements StartupTask {

    @Override
    public boolean forceRun() {
        try {
            final int pluginsCount = countPortletsById(PLUGINS.toString());
            if (pluginsCount == 0) {
                return true;
            }
            if (countPortletsById(DYNAMIC_PLUGINS.toString()) > 0) {
                return true;
            }
            return countLayoutsWithLegacyButMissingPlugins() > 0;
        } catch (final DotDataException | DotRuntimeException e) {
            Logger.error(this, String.format("An error occurred when checking the 'Plugins' portlet. "
                    + "Please verify manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    /**
     * Renames {@code dynamic-plugins} rows to {@code plugins-legacy}, then inserts {@code plugins}
     * immediately after {@code plugins-legacy} in each layout that is missing it. Safe to run more
     * than once.
     *
     * @throws DotDataException An error occurred while updating layouts.
     */
    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Adding 'plugins' Angular portlet to the admin menu");

        boolean mutated = false;

        final int dynamicPluginsCount = countPortletsById(DYNAMIC_PLUGINS.toString());
        if (dynamicPluginsCount > 0) {
            new DotConnect()
                    .setSQL("UPDATE cms_layouts_portlets SET portlet_id = ? WHERE portlet_id = ?")
                    .addParam(PLUGINS_LEGACY.toString())
                    .addParam(DYNAMIC_PLUGINS.toString())
                    .loadResult();
            Logger.info(this, "Renamed 'dynamic-plugins' to 'plugins-legacy' in cms_layouts_portlets");
            mutated = true;
        }

        final List<Map<String, Object>> legacyRows = new DotConnect()
                .setSQL("SELECT layout_id, portlet_order FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(PLUGINS_LEGACY.toString())
                .loadObjectResults();

        if (legacyRows.isEmpty()) {
            Logger.error(this, "Could not find 'plugins-legacy' in any layout. "
                    + "The 'plugins' portlet cannot be added automatically. Please add it manually.");
            if (mutated) {
                CacheLocator.getLayoutCache().clearCache();
            }
            return;
        }

        boolean anyInsert = false;
        for (final Map<String, Object> row : legacyRows) {
            final String layoutId = row.getOrDefault("layout_id", "").toString();
            if (!UtilMethods.isSet(layoutId)) {
                continue;
            }
            if (layoutHasPluginsPortlet(layoutId)) {
                continue;
            }
            final int legacyOrder = parsePortletOrder(row.get("portlet_order"));
            addPluginsPortletAfterLegacy(layoutId, legacyOrder);
            anyInsert = true;
            mutated = true;
        }

        if (anyInsert) {
            Logger.info(this, "The 'plugins' portlet has been added to the admin menu successfully!");
        } else {
            Logger.info(this, "The 'plugins' portlet is already present in all layouts that have 'plugins-legacy'.");
        }

        if (mutated) {
            CacheLocator.getLayoutCache().clearCache();
        }
    }

    private static int countPortletsById(final String portletId) {
        return new DotConnect()
                .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(portletId)
                .getInt("count");
    }

    /**
     * Layouts that have {@code plugins-legacy} but no {@code plugins} portlet in the same layout.
     */
    private static int countLayoutsWithLegacyButMissingPlugins() {
        return new DotConnect()
                .setSQL("SELECT COUNT(*) AS count FROM cms_layouts_portlets l "
                        + "WHERE l.portlet_id = ? "
                        + "AND NOT EXISTS ("
                        + "  SELECT 1 FROM cms_layouts_portlets p "
                        + "  WHERE p.layout_id = l.layout_id AND p.portlet_id = ?"
                        + ")")
                .addParam(PLUGINS_LEGACY.toString())
                .addParam(PLUGINS.toString())
                .getInt("count");
    }

    private static boolean layoutHasPluginsPortlet(final String layoutId) {
        return new DotConnect()
                .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE layout_id = ? AND portlet_id = ?")
                .addParam(layoutId)
                .addParam(PLUGINS.toString())
                .getInt("count") > 0;
    }

    private static int parsePortletOrder(final Object portletOrder) {
        if (portletOrder == null) {
            return 0;
        }
        try {
            return Integer.parseInt(portletOrder.toString());
        } catch (final NumberFormatException e) {
            Logger.warn(Task260320AddPluginsPortletToMenu.class, "Invalid portlet_order value, using 0: " + portletOrder);
            return 0;
        }
    }

    private static void addPluginsPortletAfterLegacy(final String layoutId, final int legacyOrder) throws DotDataException {
        final int pluginsOrder = legacyOrder + 1;
        new DotConnect()
                .setSQL("UPDATE cms_layouts_portlets SET portlet_order = portlet_order + 1 WHERE layout_id = ? AND portlet_order >= ?")
                .addParam(layoutId)
                .addParam(pluginsOrder)
                .loadResult();

        new DotConnect()
                .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
                .addParam(UUIDUtil.uuid())
                .addParam(layoutId)
                .addParam(PLUGINS.toString())
                .addParam(pluginsOrder)
                .loadResult();
    }

}
