package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Try;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class updates all MS-SQL "datetime without timezone" fields to timestamp with timezone
 * fields.  This allows us to store dates and times with timezone information without forcing us to
 * convert every date/time to a Relative TZ
 */
public class Task220402UpdateDateTimezones extends Task210901UpdateDateTimezones {

    /**
     * MSSQL uses these prefixes to name any generated index or constraint
     */
    private final Set<String> idxPrefixes = ImmutableSet.of("uq", "pk", "df");
    /**
     * ALTER TABLE Main statement
     */
    private static final String ALTER_TABLE_ALTER_COLUMN = "ALTER TABLE %s ALTER COLUMN %s datetimeoffset(3) ";
    /**
     * Target data-type
     */
    private static final String DATE_TIME_OFFSET = "datetimeoffset";

    private boolean filterForProvidedTableNames = false;

    /**
     * Used for testing this test
     */
    @VisibleForTesting
    public Task220402UpdateDateTimezones(
            final Map<String, Map<String, List<String>>> tableIndexConstraintRestoreScripts) {
        super();
        this.tableIndexConstraintRestoreScripts = tableIndexConstraintRestoreScripts;
        this.filterForProvidedTableNames = true;
    }

    /**
     * Default - Required by reflection for instantiation
     */
    public Task220402UpdateDateTimezones() {
    }

    /**
     * Should tell the framework that this is only meant to run for MSSQL
     */
    @Override
    public boolean forceRun() {
        return DbConnectionFactory.isMsSql();
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        tablesCount = 0;
        String tableName = null;
        try {
            for (final String table : findAllTables()) {
                tableName = table;
                if (this.updateTable(table)) {
                    tablesCount++;
                }
            }
        } catch (final Exception e) {
            throw new DotRuntimeException(String.format("An error occurred when updating table '%s': %s", tableName, e.getMessage()), e);
        }
    }

    /**
     * Overridden for testing purposes
     * @return
     * @throws SQLException
     */
    @Override
    List<String> findAllTables() throws SQLException {
        final List<String> allTables = super.findAllTables();
        allTables.removeAll(excludeTables);
        if (filterForProvidedTableNames) {
            return allTables.stream()
                    .filter(tableName -> tableIndexConstraintRestoreScripts.containsKey(tableName))
                    .collect(
                            Collectors.toList());
        }
        return allTables;
    }

    private final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();

    /**
     * Update table entry point a bool flag is returned depending on wether or not the table had
     * fields of type datetime
     */
    @Override
    boolean updateTable(final String tableName) throws SQLException {
        boolean tableUpdated = false;

        final Connection conn = DbConnectionFactory.getConnection();
        final List<String> columNames = getColumnNamesRequiredToMigrate(conn, tableName);
        if (!columNames.isEmpty()) {
            Logger.info(Task220402UpdateDateTimezones.class,
                    String.format("Updating table %s. ", tableName));
            final List<String> droppedConstraints = dropConstraintsOrIndicesIfRequired(conn,
                    tableName);
            for (final String columnName : columNames) {
                Logger.info(Task220402UpdateDateTimezones.class,
                        "updating " + tableName + "." + columnName + " to " + DATE_TIME_OFFSET);
                final String statementString = String.format(
                        ALTER_TABLE_ALTER_COLUMN, tableName, columnName);
                conn.prepareStatement(statementString).execute();
                tableUpdated = true;
            }

                final int count = droppedConstraints.isEmpty() ? 0
                        : restoreDroppedObjectsIfAny(conn, tableName);
                Logger.info(Task220402UpdateDateTimezones.class,
                        String.format("Table %s successfully updated. %d restore scripts applied.",
                                tableName, count));
            }

        return tableUpdated;
    }

    /**
     * Given the table name this will get you all the column names of the data type that needs misgration
     * @param conn
     * @param tableName
     * @return
     * @throws SQLException
     */
    private List<String> getColumnNamesRequiredToMigrate(final Connection conn,
            final String tableName) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        final ResultSet results = DotDatabaseMetaData.getColumnsMetaData(conn, tableName);
        while (results.next()) {
            final String typeName = results.getString("TYPE_NAME").toLowerCase();
            if (!DATE_TIME_OFFSET.equals(typeName) && typeName.startsWith("datetime")) {
                final String columnName = results.getString("COLUMN_NAME");
                columnNames.add(columnName);
            }
        }
        return columnNames;
    }

    /**
     * This method extracts both indices and constraints associated with the given table and uses the main registration script structure to determine what needs to be worked
     * @param conn
     * @param tableName
     * @return
     */
    private List<String> dropConstraintsOrIndicesIfRequired(final Connection conn,
            final String tableName) {

        final List<String> constraintsOrIndices = new ArrayList<>();

        final List<String> indices = (Try.of(
                        () -> dotDatabaseMetaData.getIndices(tableName))
                .getOrElse(ImmutableList.of())).stream().map(String::toLowerCase).collect(
                Collectors.toList());

        final List<String> generatedIndices = indices.stream()
                .filter(idx -> Try.of(() -> idxPrefixes.contains(idx.substring(0, 2)))
                        .getOrElse(false)).collect(Collectors.toList());

        indices.removeAll(generatedIndices);

        final List<String> constraints = (Try.of(
                        () -> dotDatabaseMetaData.getConstraints(tableName))
                .getOrElse(ImmutableList.of())).stream().map(String::toLowerCase).collect(
                Collectors.toList());

        constraints.addAll(generatedIndices);

        //These are the objects we know present a conflict when doing the alter table
        final Map<String, List<String>> restoreScriptsByObjectName = tableIndexConstraintRestoreScripts.get(
                tableName);

        if (null != restoreScriptsByObjectName) {
            dropConstraints(conn, tableName, constraintsOrIndices, restoreScriptsByObjectName,
                    constraints);
            dropIndices(conn, tableName, constraintsOrIndices, restoreScriptsByObjectName, indices);
        }

        return constraintsOrIndices;
    }

    /**
     * Drop constraint method
     * @param conn
     * @param tableName
     * @param constraintsOrIndices
     * @param restoreScriptsByObjectName
     * @param constraints
     */
    private void dropConstraints(Connection conn, String tableName,
            List<String> constraintsOrIndices,
            Map<String, List<String>> restoreScriptsByObjectName, List<String> constraints) {
        for (final String constraintName : constraints) {
            //so if we're looking at one of the conflicting objects we need to drop it
            if (hasMatchingScript(restoreScriptsByObjectName, constraintName)) {

                Logger.info(Task220402UpdateDateTimezones.class,
                        () -> String.format("Attempting to drop constraint `%s`. ",
                                constraintName));

                final boolean dropped = Try.of(() -> {
                    dotDatabaseMetaData.executeDropConstraint(conn, tableName, constraintName);
                    return true;
                }).getOrElse(false);

                if (!dropped) {
                    Logger.info(Task220402UpdateDateTimezones.class,
                            () -> String.format("Unable to drop constraint `%s`. ",
                                    constraintName));
                    continue;
                }
                constraintsOrIndices.add(constraintName);
            }
        }
    }

    /**
     * Drop indices method
     * @param conn
     * @param tableName
     * @param constraintsOrIndices
     * @param restoreScriptsByObjectName
     * @param indices
     */
    private void dropIndices(Connection conn, String tableName, List<String> constraintsOrIndices,
            Map<String, List<String>> restoreScriptsByObjectName, List<String> indices) {
        for (final String indexName : indices) {
            //so if we're looking at one of the conflicting objects we need to drop it
            if (hasMatchingScript(restoreScriptsByObjectName, indexName)) {

                Logger.info(Task220402UpdateDateTimezones.class,
                        () -> String.format("Attempting to drop index `%s`. ", indexName));

                final boolean dropped = Try.of(() -> {
                    dotDatabaseMetaData.dropIndex(conn, tableName, indexName);
                    return true;
                }).getOrElse(false);

                if (!dropped) {
                    Logger.info(Task220402UpdateDateTimezones.class,
                            () -> String.format("Unable to drop index `%s`. ", indexName));
                    continue;
                }
                constraintsOrIndices.add(indexName);
            }
        }
    }

    /**
     * With this method we look into the main structure defined below to determine if there's any given script to be applied for a given constraint name/prefix
     * @param restoreScriptsByObjectName
     * @param constraintName
     * @return
     */
    private boolean hasMatchingScript(final Map<String, List<String>> restoreScriptsByObjectName,
            final String constraintName) {
        return restoreScriptsByObjectName.containsKey(constraintName)
                || restoreScriptsByObjectName.keySet().stream().map(String::toLowerCase)
                .anyMatch(constraintName::startsWith);
    }

    /**
     * Given a table name this method looks for all the associated restore script and apply them
     * @param conn
     * @param tableName
     * @return
     * @throws SQLException
     */
    private int restoreDroppedObjectsIfAny(final Connection conn, final String tableName)
            throws SQLException {
        //These are scripts to restore any object previously dropped organized by object name (index or constraint)
        final Map<String, List<String>> restoreScriptsByObjectName = tableIndexConstraintRestoreScripts.get(
                tableName);
        if (null == restoreScriptsByObjectName) {
            Logger.warn(Task220402UpdateDateTimezones.class,
                    () -> "No scripts found for table " + tableName);
            return 0;
        }
        int count = 0;
        for (final Map.Entry<String, List<String>> entry : restoreScriptsByObjectName.entrySet()) {
            final List<String> scripts = entry.getValue();
            for (final String script : scripts) {
                conn.prepareStatement(script).execute();
                count++;
            }
        }
        return count;
    }

    /**
     * This structure is organized like this: Table name - then constraints/indices and for each one
     * of them a List of scripts required to restore the old functionality PLEASE NOTICE everything
     * on this map is in lowercase! The object name is added explicitly when know for example
     * idx_analytic_summary_visits_2 that's a known index Other objects are created by the db engine
     * using random names preceded by one of the following prefixes (df,uq,pk) `df` represents an
     * object generated when the keyword DEFAULT is present e.g. mod_date datetime NOT NULL DEFAULT
     * GETDATE() 'uq' represents an object generated when the keyword UNIQUE is present e.g.  UNIQUE
     * (language_code,add_date) `pk` represents an object generated when the keyword PRIMARY KEY is
     * present e.g. PRIMARY KEY (id)
     */
    Map<String, Map<String, List<String>>> tableIndexConstraintRestoreScripts =
            new ImmutableMap.Builder<String, Map<String, List<String>>>()
                    .put("sitesearch_audit", ImmutableMap.of("pk",
                                    ImmutableList.of(
                                            "alter table sitesearch_audit alter column job_id NVARCHAR(36) not null",
                                            "alter table sitesearch_audit alter column fire_date datetime not null",
                                            "alter table sitesearch_audit add primary key (job_id, fire_date) ")
                            )
                    )
                    .put("analytic_summary_period", ImmutableMap.of("uq",
                                    ImmutableList.of(
                                            "alter table analytic_summary_period ADD CONSTRAINT uq_analytic_summary_period_full_date UNIQUE  (full_date)")
                            )
                    )
                    .put("analytic_summary_visits", ImmutableMap.of("idx_analytic_summary_visits_2",
                                    ImmutableList.of(
                                            "create index idx_analytic_summary_visits_2 on analytic_summary_visits (visit_time)")
                            )
                    )
                    .put("analytic_summary_workstream",
                            ImmutableMap.of("idx_dashboard_workstream_3", ImmutableList.of(
                                    "create index idx_dashboard_workstream_3 on analytic_summary_workstream (mod_date)")
                            )
                    )
                    .put("api_token_issued",
                            ImmutableMap.of("df", ImmutableList.of(
                                            "alter table api_token_issued add constraint df_api_token_issued_revoke_date DEFAULT getDate() FOR revoke_date"
                                    )
                            )
                    )
                    .put("campaign",
                            ImmutableMap.of(
                                    "idx_campaign_2", ImmutableList.of(
                                            "create index idx_campaign_2 on campaign (start_date)"
                                    ),
                                    "idx_campaign_3", ImmutableList.of(
                                            "create index idx_campaign_3 on campaign (completed_date)"
                                    ),
                                    "idx_campaign_4", ImmutableList.of(
                                            "create index idx_campaign_4 on campaign (expiration_date)"
                                    )
                            )
                    )
                    .put("clickstream_request", ImmutableMap.of(
                                    "idx_user_clickstream_request_4", ImmutableList.of(
                                            "create index idx_user_clickstream_request_4 on clickstream_request (timestampper)")
                            )
                    )
                    .put("container_version_info", ImmutableMap.of(
                                    "idx_container_vi_version_ts", ImmutableList.of(
                                            "create index idx_container_vi_version_ts on container_version_info(version_ts)")
                            )
                    )
                    .put("contentlet_version_info", ImmutableMap.of("idx_contentlet_vi_version_ts",
                                    ImmutableList.of(
                                            "create index idx_contentlet_vi_version_ts on contentlet_version_info(version_ts)")
                            )
                    )
                    .put("dist_reindex_journal",
                            ImmutableMap.of("df",
                                    ImmutableList.of(
                                            "alter table dist_reindex_journal add constraint df_dist_reindex_journal_time_entered DEFAULT getDate() FOR time_entered"
                                    ),
                                    "dist_reindex_index5", ImmutableList.of(
                                            "create index dist_reindex_index5 ON dist_reindex_journal (priority, time_entered)"
                                    )
                            )
                    )
                    .put("identifier",
                            ImmutableMap.of("idx_identifier_pub",
                                    ImmutableList.of(
                                            "create index idx_identifier_pub on identifier (syspublish_date)"
                                    ),
                                    "idx_identifier_exp",
                                    ImmutableList.of(
                                            "create index idx_identifier_exp on identifier (sysexpire_date)"
                                    )
                            )
                    )
                    .put("link_version_info",
                            ImmutableMap.of("idx_link_vi_version_ts",
                                    ImmutableList.of(
                                            "create index idx_link_vi_version_ts on link_version_info(version_ts)")
                            )
                    )
                    .put("recipient",
                            ImmutableMap.of("idx_recipiets_2",
                                    ImmutableList.of(
                                            "create index idx_recipiets_2 on recipient (sent)")
                            )
                    )
                    .put("storage",
                            ImmutableMap.of("df",
                                    ImmutableList.of(
                                            "alter table storage add constraint df_storage_mod_date DEFAULT getDate() FOR mod_date")
                            )
                    )
                    .put("storage_data",
                            ImmutableMap.of("df",
                                    ImmutableList.of(
                                            "alter table storage_data add constraint df_storage_data_mod_date DEFAULT getDate() FOR mod_date")
                            )
                    )
                    .put("storage_group",
                            ImmutableMap.of("df",
                                    ImmutableList.of(
                                            "alter table storage_group add constraint df_storage_group_mod_date DEFAULT getDate() FOR mod_date")
                            )
                    )
                    .put("storage_x_data",
                            ImmutableMap.of("df",
                                    ImmutableList.of(
                                            "alter table storage_x_data add constraint df_storage_x_data_mod_date DEFAULT getDate() FOR mod_date")
                            )
                    )
                    .put("template_version_info",
                            ImmutableMap.of("idx_template_vi_version_ts",
                                    ImmutableList.of(
                                            "create index idx_template_vi_version_ts on template_version_info(version_ts)")
                            )
                    )
                    .build();

    final Set<String> excludeTables = ImmutableSet.of("calendar_reminder");

}
