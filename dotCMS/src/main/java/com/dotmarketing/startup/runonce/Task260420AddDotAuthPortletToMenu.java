package com.dotmarketing.startup.runonce;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dotmarketing.util.PortletID.DOT_AUTH;

/**
 * Adds the {@code dotAuth} portlet to the admin menu. The portlet is inserted
 * into every layout that contains the Apps portlet (typically
 * "Settings → Configuration") at the position immediately after Apps, since
 * {@code dotAuth} is the dedicated editor for the OAuth app and conceptually
 * sits alongside it.
 * <p>
 * This is registered as a <b>run-always</b> task, not a version-gated run-once
 * upgrade: the bundled starter records a {@code db_version} newer than this
 * task's number while its exported layouts predate the portlet, so a run-once
 * task never fires on fresh installs. {@link #forceRun()} is a cheap COUNT that
 * returns {@code false} once every Apps layout already has {@code dotAuth}, so
 * steady-state startups skip the insert. Note this also means removing the
 * portlet from an Apps layout re-adds it on the next restart — remove it via
 * Roles &amp; Tools from a layout without Apps, or accept the reconcile.
 * <p>
 * Layouts that already contain the {@code dotAuth} portlet are skipped so the
 * task is safe to re-run and safe in clustered startup where multiple nodes
 * may race.
 * <p>
 * If the Apps portlet cannot be located in any layout, the task logs a warning
 * and skips the insertion — administrators can add it manually through the
 * Roles &amp; Tools UI.
 *
 * @since Apr 20th, 2026
 */
public class Task260420AddDotAuthPortletToMenu implements StartupTask {

    private static final String APPS_PORTLET_ID = "apps";

    private static final String SQL_COUNT_PENDING =
            "SELECT COUNT(DISTINCT apps.layout_id) AS count "
            + "FROM cms_layouts_portlets apps "
            + "WHERE apps.portlet_id = ? "
            + "AND NOT EXISTS ("
            + "  SELECT 1 FROM cms_layouts_portlets existing"
            + "  WHERE existing.layout_id = apps.layout_id"
            + "  AND existing.portlet_id = ?)";

    private static final String SQL_FIND_APPS_LAYOUTS =
            "SELECT layout_id, portlet_order FROM cms_layouts_portlets "
            + "WHERE portlet_id = ? ORDER BY layout_id, portlet_order";

    private static final String SQL_COUNT_EXISTING =
            "SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets "
            + "WHERE layout_id = ? AND portlet_id = ?";

    private static final String SQL_SHIFT_ORDER =
            "UPDATE cms_layouts_portlets SET portlet_order = portlet_order + 1 "
            + "WHERE layout_id = ? AND portlet_order >= ?";

    private static final String SQL_INSERT =
            "INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) "
            + "VALUES (?, ?, ?, ?)";

    @Override
    public boolean forceRun() {
        try {
            final int pendingCount = new DotConnect()
                    .setSQL(SQL_COUNT_PENDING)
                    .addParam(APPS_PORTLET_ID)
                    .addParam(DOT_AUTH.toString())
                    .getInt("count");
            return pendingCount > 0;
        } catch (final Exception e) {
            // swallowing here would record the task as skipped-clean and the portlet would
            // silently never be added — fail loudly so startup surfaces the real DB problem
            throw new DotRuntimeException(String.format("An error occurred when checking the 'dotAuth' portlet: %s",
                    ExceptionUtil.getErrorMessage(e)), e);
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Adding 'dotAuth' portlet to the admin menu next to 'apps'");

        final List<Map<String, Object>> appsLayouts = new DotConnect()
                .setSQL(SQL_FIND_APPS_LAYOUTS)
                .addParam(APPS_PORTLET_ID)
                .loadObjectResults();

        if (appsLayouts.isEmpty()) {
            Logger.warn(this, "Could not find the 'apps' portlet in any layout. " +
                    "The 'dotAuth' portlet cannot be added automatically. Please add it manually.");
            return;
        }

        int inserted = 0;
        int skipped = 0;
        for (final Map<String, Object> row : appsLayouts) {
            // getOrDefault doesn't help here: the columns can be present but null
            final String layoutId = Objects.toString(row.get("layout_id"), "");
            if (!UtilMethods.isSet(layoutId)) {
                continue;
            }

            final int existingDotAuth = new DotConnect()
                    .setSQL(SQL_COUNT_EXISTING)
                    .addParam(layoutId)
                    .addParam(DOT_AUTH.toString())
                    .getInt("count");
            if (existingDotAuth > 0) {
                skipped++;
                continue;
            }

            final int appsOrder = Integer.parseInt(Objects.toString(row.get("portlet_order"), "0"));
            final int dotAuthOrder = appsOrder + 1;

            try {
                LocalTransaction.wrap(() -> {
                    new DotConnect()
                            .setSQL(SQL_SHIFT_ORDER)
                            .addParam(layoutId)
                            .addParam(dotAuthOrder)
                            .loadResult();

                    new DotConnect()
                            .setSQL(SQL_INSERT)
                            .addParam(UUIDUtil.uuid())
                            .addParam(layoutId)
                            .addParam(DOT_AUTH.toString())
                            .addParam(dotAuthOrder)
                            .loadResult();
                });
            } catch (final com.dotmarketing.exception.DotSecurityException e) {
                throw new DotDataException(e.getMessage(), e);
            }

            Logger.info(this, "Added 'dotAuth' portlet at position " + dotAuthOrder + " in layout: " + layoutId);
            inserted++;
        }

        CacheLocator.getLayoutCache().clearCache();
        Logger.info(this, String.format(
                "The 'dotAuth' portlet startup task finished — inserted into %d layout(s), skipped %d (already present).",
                inserted, skipped));
    }

}
