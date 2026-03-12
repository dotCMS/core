package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.util.List;
import java.util.Map;

/**
 * Upgrade task that iterates all host (Site) identifier records and syncs the
 * {@code identifier.asset_name} column to the contentlet's {@code hostName} field value.
 *
 * <p>This is required by the nestable-hosts feature (AC 19b), which relies on
 * {@code identifier.asset_name == hostname} so that URL path segments can be resolved
 * directly from the identifier table without additional joins.
 *
 * <p>The task is idempotent: rows where {@code asset_name} already equals the hostname are
 * left untouched.  The System Host sentinel record ({@code id = 'SYSTEM_HOST'}) is excluded
 * from the update because it is a virtual/sentinel object that has no real contentlet backing.
 *
 * <p>The hostname value is read preferentially from {@code contentlet_as_json} (PostgreSQL or
 * MSSQL JSON functions) and falls back to the legacy dynamic column (e.g. {@code text1})
 * discovered via the {@code field} metadata table.
 *
 * @author dotCMS
 * @since Mar 13th, 2026
 */
public class Task260313SyncHostIdentifierAssetName implements StartupTask {

    /**
     * Query that retrieves the dynamic {@code field_contentlet} column name (e.g. {@code text1})
     * that stores the {@code hostName} value in the {@code contentlet} table for the Host content
     * type.
     */
    static final String GET_HOSTNAME_COLUMN =
            "SELECT f.field_contentlet "
            + "FROM field f "
            + "JOIN structure s ON s.inode = f.structure_inode "
            + "WHERE s.velocity_var_name = 'Host' "
            + "  AND f.velocity_var_name = 'hostName'";

    /**
     * PostgreSQL COUNT query.  Counts host identifier records whose {@code asset_name} is
     * out-of-sync with the hostname stored in the contentlet.  The System Host sentinel is
     * excluded.  The {@code %s} placeholder is replaced with the dynamic hostname expression.
     */
    static final String COUNT_HOSTS_NEEDING_SYNC_PG =
            "SELECT COUNT(i.id) AS total "
            + "FROM identifier i "
            + "JOIN contentlet c ON c.identifier = i.id "
            + "JOIN contentlet_version_info cvi ON cvi.working_inode = c.inode "
            + "JOIN structure s ON s.inode = c.structure_inode "
            + "WHERE s.velocity_var_name = 'Host' "
            + "  AND i.asset_type    = 'contentlet' "
            + "  AND i.asset_subtype = 'Host' "
            + "  AND i.id <> 'SYSTEM_HOST' "
            + "  AND i.asset_name IS DISTINCT FROM %s "
            + "  AND %s IS NOT NULL "
            + "  AND %s <> ''";

    /** MSSQL variant – uses {@code <>} instead of {@code IS DISTINCT FROM}. */
    static final String COUNT_HOSTS_NEEDING_SYNC_MSSQL =
            "SELECT COUNT(i.id) AS total "
            + "FROM identifier i "
            + "JOIN contentlet c ON c.identifier = i.id "
            + "JOIN contentlet_version_info cvi ON cvi.working_inode = c.inode "
            + "JOIN structure s ON s.inode = c.structure_inode "
            + "WHERE s.velocity_var_name = 'Host' "
            + "  AND i.asset_type    = 'contentlet' "
            + "  AND i.asset_subtype = 'Host' "
            + "  AND i.id <> 'SYSTEM_HOST' "
            + "  AND (i.asset_name <> %s OR i.asset_name IS NULL) "
            + "  AND %s IS NOT NULL "
            + "  AND %s <> ''";

    /**
     * PostgreSQL UPDATE: sets {@code asset_name} to the hostname for all out-of-sync host
     * identifiers.  The {@code %s} placeholder is replaced with the dynamic hostname expression.
     */
    static final String UPDATE_HOST_ASSET_NAME_PG =
            "UPDATE identifier i "
            + "SET asset_name = %s "
            + "FROM contentlet c "
            + "JOIN contentlet_version_info cvi ON cvi.working_inode = c.inode "
            + "JOIN structure s ON s.inode = c.structure_inode "
            + "WHERE s.velocity_var_name = 'Host' "
            + "  AND i.id = c.identifier "
            + "  AND i.asset_type    = 'contentlet' "
            + "  AND i.asset_subtype = 'Host' "
            + "  AND i.id <> 'SYSTEM_HOST' "
            + "  AND i.asset_name IS DISTINCT FROM %s "
            + "  AND %s IS NOT NULL "
            + "  AND %s <> ''";

    /**
     * MSSQL UPDATE variant.  The {@code %s} placeholder is replaced with the dynamic hostname
     * expression.
     */
    static final String UPDATE_HOST_ASSET_NAME_MSSQL =
            "UPDATE i "
            + "SET i.asset_name = %s "
            + "FROM identifier i "
            + "JOIN contentlet c ON c.identifier = i.id "
            + "JOIN contentlet_version_info cvi ON cvi.working_inode = c.inode "
            + "JOIN structure s ON s.inode = c.structure_inode "
            + "WHERE s.velocity_var_name = 'Host' "
            + "  AND i.asset_type    = 'contentlet' "
            + "  AND i.asset_subtype = 'Host' "
            + "  AND i.id <> 'SYSTEM_HOST' "
            + "  AND (i.asset_name <> %s OR i.asset_name IS NULL) "
            + "  AND %s IS NOT NULL "
            + "  AND %s <> ''";

    /**
     * Returns {@code true} when at least one host identifier has an {@code asset_name} that
     * differs from the hostname stored in the corresponding contentlet.
     *
     * <p>If the Host content type / hostName field metadata cannot be located (e.g. very old
     * schema), the method returns {@code false} so the upgrade step is safely skipped.
     */
    @Override
    public boolean forceRun() {
        try {
            final String hostnameExpr = buildHostnameExpression();
            if (hostnameExpr == null) {
                // Host content type or hostName field not found – nothing to sync.
                return false;
            }
            final String countSql = buildCountSql(hostnameExpr);
            final int total = new DotConnect().setSQL(countSql).getInt("total");
            if (total > 0) {
                Logger.info(this, String.format(
                        "Found %d host identifier(s) whose asset_name is out-of-sync with their "
                        + "hostname. Sync will be performed.", total));
            }
            return total > 0;
        } catch (final Exception e) {
            Logger.error(this,
                    "Error checking host identifiers for asset_name sync. "
                    + "Task will be skipped: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Updates {@code identifier.asset_name} to equal the hostname for every host identifier
     * record that is currently out-of-sync.
     *
     * @throws DotDataException    if the database update fails.
     * @throws DotRuntimeException if a runtime error occurs.
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final String hostnameExpr = buildHostnameExpression();
        if (hostnameExpr == null) {
            Logger.warn(this,
                    "Could not determine hostName column for Host content type. "
                    + "Skipping asset_name sync.");
            return;
        }

        final String updateSql = buildUpdateSql(hostnameExpr);
        Logger.info(this, "Syncing identifier.asset_name for all host records...");
        try {
            new DotConnect().executeStatement(updateSql);
            Logger.info(this,
                    "identifier.asset_name sync for host records completed successfully.");
        } catch (final Exception e) {
            throw new DotDataException(
                    "Failed to sync identifier.asset_name for host records: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // package-private helpers (visible for unit tests)
    // -------------------------------------------------------------------------

    /**
     * Returns the SQL expression that evaluates to the hostname string for a given contentlet row.
     *
     * <p>The expression prefers the JSON column ({@code contentlet_as_json}) when available
     * (PostgreSQL / MSSQL), falling back to the legacy dynamic column (e.g. {@code text1})
     * discovered from the {@code field} metadata table.
     *
     * @return hostname SQL expression, or {@code null} if the field metadata cannot be found.
     */
    String buildHostnameExpression() {
        try {
            final List<Map<String, Object>> results =
                    new DotConnect().setSQL(GET_HOSTNAME_COLUMN).loadObjectResults();
            if (results.isEmpty()) {
                Logger.warn(this,
                        "No 'hostName' field found on Host content type in field table. "
                        + "Cannot build hostname expression.");
                return null;
            }
            final String legacyCol = "c." + results.get(0).get("field_contentlet").toString();

            if (DbConnectionFactory.isPostgres()) {
                // Prefer JSON; fall back to legacy column for older rows without JSON data.
                return String.format(
                        "COALESCE(NULLIF(c.contentlet_as_json->'fields'->'hostName'->>'value', ''), %s)",
                        legacyCol);
            } else if (DbConnectionFactory.isMsSql()) {
                return String.format(
                        "COALESCE(NULLIF(JSON_VALUE(c.contentlet_as_json, '$.fields.hostName.value'), ''), %s)",
                        legacyCol);
            } else {
                // MySQL / other: only the legacy column is available.
                return legacyCol;
            }
        } catch (final DotDataException e) {
            Logger.error(this,
                    "Error querying hostName field metadata: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Builds the COUNT query used by {@link #forceRun()} to detect out-of-sync host identifiers.
     *
     * @param hostnameExpr the SQL expression that evaluates to the hostname.
     * @return the complete COUNT SQL statement.
     */
    private String buildCountSql(final String hostnameExpr) {
        final String template = DbConnectionFactory.isMsSql()
                ? COUNT_HOSTS_NEEDING_SYNC_MSSQL
                : COUNT_HOSTS_NEEDING_SYNC_PG;
        return String.format(template, hostnameExpr, hostnameExpr, hostnameExpr);
    }

    /**
     * Builds the UPDATE statement used by {@link #executeUpgrade()} to sync
     * {@code identifier.asset_name} to the hostname value.
     *
     * @param hostnameExpr the SQL expression that evaluates to the hostname.
     * @return the complete UPDATE SQL statement.
     */
    private String buildUpdateSql(final String hostnameExpr) {
        final String template = DbConnectionFactory.isMsSql()
                ? UPDATE_HOST_ASSET_NAME_MSSQL
                : UPDATE_HOST_ASSET_NAME_PG;
        // Both templates have 4 occurrences of the hostname expression placeholder.
        return String.format(template,
                hostnameExpr, hostnameExpr, hostnameExpr, hostnameExpr);
    }

}
