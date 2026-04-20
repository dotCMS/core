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

import static com.dotmarketing.util.PortletID.DOT_AUTH;

/**
 * Adds the {@code dotAuth} portlet to the admin menu. The portlet is inserted
 * into the same layout as the Apps portlet (typically "Settings → Configuration")
 * at the position immediately after Apps, since {@code dotAuth} is the dedicated
 * editor for the OAuth app and conceptually sits alongside it.
 * <p>
 * If the Apps portlet cannot be located in any layout, the task logs a warning
 * and skips the insertion — administrators can add it manually through the
 * Roles & Tools UI.
 *
 * @since Apr 20th, 2026
 */
public class Task260420AddDotAuthPortletToMenu implements StartupTask {

    private static final String APPS_PORTLET_ID = "apps";

    @Override
    public boolean forceRun() {
        try {
            final int dotAuthCount = new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE portlet_id = ?")
                    .addParam(DOT_AUTH.toString())
                    .getInt("count");
            return dotAuthCount == 0;
        } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when checking the 'dotAuth' portlet. " +
                    "Please verify manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Adding 'dotAuth' portlet to the admin menu next to 'apps'");

        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT layout_id, portlet_order FROM cms_layouts_portlets WHERE portlet_id = ? ORDER BY portlet_order")
                .addParam(APPS_PORTLET_ID)
                .loadObjectResults();

        if (results.isEmpty() || !UtilMethods.isSet(results.get(0).getOrDefault("layout_id", "").toString())) {
            Logger.warn(this, "Could not find the 'apps' portlet in any layout. " +
                    "The 'dotAuth' portlet cannot be added automatically. Please add it manually.");
            return;
        }

        final String layoutId = results.get(0).get("layout_id").toString();
        final int appsOrder = Integer.parseInt(results.get(0).getOrDefault("portlet_order", "0").toString());
        final int dotAuthOrder = appsOrder + 1;

        new DotConnect()
                .setSQL("UPDATE cms_layouts_portlets SET portlet_order = portlet_order + 1 " +
                        "WHERE layout_id = ? AND portlet_order >= ?")
                .addParam(layoutId)
                .addParam(dotAuthOrder)
                .loadResult();

        new DotConnect()
                .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
                .addParam(UUIDUtil.uuid())
                .addParam(layoutId)
                .addParam(DOT_AUTH.toString())
                .addParam(dotAuthOrder)
                .loadResult();

        Logger.info(this, "Added 'dotAuth' portlet at position " + dotAuthOrder + " in layout: " + layoutId);

        CacheLocator.getLayoutCache().clearCache();
        Logger.info(this, "The 'dotAuth' portlet has been added to the admin menu successfully!");
    }

}
