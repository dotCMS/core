package com.dotmarketing.startup.runonce;

import static com.dotmarketing.util.PortletID.ROLES;
import static com.dotmarketing.util.PortletID.TOOLS;
import static com.dotmarketing.util.PortletID.USERS;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Moves the admin menu onto the promoted Angular Users, Roles and Tools portlets.
 * <ol>
 *     <li>Rows that still point at the retired {@code users-beta}, {@code roles-beta} or
 *     {@code tools-beta} ids become {@code users}, {@code roles} and {@code tools}, keeping their
 *     position. Where the layout already holds the promoted id, the beta row is deleted instead,
 *     since {@code cms_layouts_portlets} allows a portlet only once per layout.</li>
 *     <li>Every layout that has Roles but no Tools gets Tools right after Roles; the rows below
 *     Roles move down one place. Legacy Tools was a tab inside Roles, so no layout has a Tools row
 *     until this task adds it.</li>
 * </ol>
 * Rows that already point at {@code users} or {@code roles} are never touched: those ids now
 * name the Angular portlets, so every existing menu shows them with no data change.
 * <p>
 * This is registered as a <b>run-always</b> task, not a version-gated run-once upgrade, for the
 * same reason as {@link Task260420AddDotAuthPortletToMenu}: the bundled starter records a
 * {@code db_version} newer than this task while its layouts have no Tools row, so a run-once task
 * would never fire on a fresh install. {@link #forceRun()} is two cheap counts that return
 * {@code false} once everything is reconciled. It also means that removing Tools from a section
 * that still has Roles re-adds it on the next restart.
 * <p>
 * Rollback: the Tools insert is additive and older versions skip portlet ids they do not know.
 * The beta-row conversion is one-way: after a rollback, a section that had opted into a beta
 * portlet keeps {@code users} / {@code roles}, which older versions render as the old screens.
 *
 * @since Nov 10th, 2026
 */
public class Task261110PromoteUsersRolesToolsPortlets implements StartupTask {

    /** Each retired beta id and the promoted id that replaces it. */
    private static final Map<String, String> BETA_TO_PROMOTED = Map.of(
            "users-beta", USERS.toString(),
            "roles-beta", ROLES.toString(),
            "tools-beta", TOOLS.toString());

    private static final String SQL_COUNT_BETA_ROWS =
            "SELECT COUNT(*) AS count FROM cms_layouts_portlets WHERE portlet_id IN (?, ?, ?)";

    private static final String SQL_COUNT_ROLES_WITHOUT_TOOLS =
            "SELECT COUNT(DISTINCT roles.layout_id) AS count "
            + "FROM cms_layouts_portlets roles "
            + "WHERE roles.portlet_id = ? "
            + "AND NOT EXISTS ("
            + "  SELECT 1 FROM cms_layouts_portlets existing"
            + "  WHERE existing.layout_id = roles.layout_id"
            + "  AND existing.portlet_id = ?)";

    private static final String SQL_RENAME_BETA =
            "UPDATE cms_layouts_portlets SET portlet_id = ? "
            + "WHERE portlet_id = ? "
            + "AND layout_id NOT IN ("
            + "  SELECT layout_id FROM cms_layouts_portlets WHERE portlet_id = ?)";

    private static final String SQL_DELETE_BETA =
            "DELETE FROM cms_layouts_portlets WHERE portlet_id = ?";

    private static final String SQL_FIND_ROLES_WITHOUT_TOOLS =
            "SELECT roles.layout_id, roles.portlet_order "
            + "FROM cms_layouts_portlets roles "
            + "WHERE roles.portlet_id = ? "
            + "AND NOT EXISTS ("
            + "  SELECT 1 FROM cms_layouts_portlets existing"
            + "  WHERE existing.layout_id = roles.layout_id"
            + "  AND existing.portlet_id = ?) "
            + "ORDER BY roles.layout_id";

    private static final String SQL_SHIFT_ORDER =
            "UPDATE cms_layouts_portlets SET portlet_order = portlet_order + 1 "
            + "WHERE layout_id = ? AND portlet_order >= ?";

    private static final String SQL_INSERT =
            "INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) "
            + "VALUES (?, ?, ?, ?)";

    /**
     * Tells whether a beta row is left anywhere, or a layout has Roles but no Tools.
     *
     * @return {@code true} when there is something to convert or insert
     * @throws DotRuntimeException if the counts cannot be read; swallowing the error would record
     *                             the task as done and the menu would silently never change
     */
    @Override
    public boolean forceRun() {
        try {
            final int betaRows = new DotConnect()
                    .setSQL(SQL_COUNT_BETA_ROWS)
                    .addParam("users-beta")
                    .addParam("roles-beta")
                    .addParam("tools-beta")
                    .getInt("count");
            if (betaRows > 0) {
                return true;
            }
            final int rolesWithoutTools = new DotConnect()
                    .setSQL(SQL_COUNT_ROLES_WITHOUT_TOOLS)
                    .addParam(ROLES.toString())
                    .addParam(TOOLS.toString())
                    .getInt("count");
            return rolesWithoutTools > 0;
        } catch (final Exception e) {
            throw new DotRuntimeException(String.format(
                    "An error occurred when checking the Users, Roles and Tools menu rows: %s",
                    ExceptionUtil.getErrorMessage(e)), e);
        }
    }

    /**
     * Converts the beta rows, then inserts Tools after Roles where it is missing, and clears the
     * layout cache so open sessions pick up the new menu.
     *
     * @throws DotDataException if a row cannot be read or written
     */
    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Moving the admin menu onto the promoted Users, Roles and Tools portlets");
        convertBetaRows();
        final int inserted = insertToolsAfterRoles();
        CacheLocator.getLayoutCache().clearCache();
        Logger.info(this, String.format(
                "The Users, Roles and Tools menu task finished: Tools inserted into %d layout(s).", inserted));
    }

    /**
     * Turns every beta row into its promoted id, or deletes it where the layout already holds that
     * id. Runs in one transaction, so a failure leaves no half-converted menu.
     *
     * @throws DotDataException if the rows cannot be written
     */
    private void convertBetaRows() throws DotDataException {
        try {
            LocalTransaction.wrap(() -> {
                for (final Map.Entry<String, String> entry : BETA_TO_PROMOTED.entrySet()) {
                    final String betaId = entry.getKey();
                    final String promotedId = entry.getValue();
                    new DotConnect()
                            .setSQL(SQL_RENAME_BETA)
                            .addParam(promotedId)
                            .addParam(betaId)
                            .addParam(promotedId)
                            .loadResult();
                    // what is left sits in a layout that already holds the promoted id
                    new DotConnect()
                            .setSQL(SQL_DELETE_BETA)
                            .addParam(betaId)
                            .loadResult();
                }
            });
        } catch (final DotSecurityException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    /**
     * Inserts Tools right after Roles in every layout that has Roles but no Tools, moving the rows
     * below Roles down one place. Each layout is changed in its own transaction.
     *
     * @return how many layouts received Tools
     * @throws DotDataException if a layout cannot be read or written
     */
    private int insertToolsAfterRoles() throws DotDataException {
        final List<Map<String, Object>> rolesLayouts = new DotConnect()
                .setSQL(SQL_FIND_ROLES_WITHOUT_TOOLS)
                .addParam(ROLES.toString())
                .addParam(TOOLS.toString())
                .loadObjectResults();

        int inserted = 0;
        for (final Map<String, Object> row : rolesLayouts) {
            // getOrDefault doesn't help here: the columns can be present but null
            final String layoutId = Objects.toString(row.get("layout_id"), "");
            if (!UtilMethods.isSet(layoutId)) {
                continue;
            }
            final int rolesOrder = Integer.parseInt(Objects.toString(row.get("portlet_order"), "0"));
            if (addToolsAfterRoles(layoutId, rolesOrder)) {
                inserted++;
            }
        }
        return inserted;
    }

    /**
     * Inserts Tools right after Roles in one layout, moving the rows below Roles down one place,
     * in its own transaction.
     *
     * @param layoutId   the layout that receives Tools
     * @param rolesOrder the position of Roles in that layout
     * @return {@code true} when Tools was inserted
     * @throws DotDataException if the layout cannot be written
     */
    @VisibleForTesting
    boolean addToolsAfterRoles(final String layoutId, final int rolesOrder) throws DotDataException {
        final int toolsOrder = rolesOrder + 1;
        try {
            LocalTransaction.wrap(() -> {
                new DotConnect()
                        .setSQL(SQL_SHIFT_ORDER)
                        .addParam(layoutId)
                        .addParam(toolsOrder)
                        .loadResult();
                new DotConnect()
                        .setSQL(SQL_INSERT)
                        .addParam(UUIDUtil.uuid())
                        .addParam(layoutId)
                        .addParam(TOOLS.toString())
                        .addParam(toolsOrder)
                        .loadResult();
            });
        } catch (final DotSecurityException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        Logger.info(this, "Added the 'tools' portlet at position " + toolsOrder + " in layout: " + layoutId);
        return true;
    }
}
