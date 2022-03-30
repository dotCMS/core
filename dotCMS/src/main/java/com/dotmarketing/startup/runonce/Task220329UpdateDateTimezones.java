package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.vavr.control.Try;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * This class updates all MS-SQL "datetime without timezone" fields to timestamp with timezone
 * fields.  This allows us to store dates and times with timezone information without forcing us to
 * convert every date/time to a Relative TZ
 */
public class Task220329UpdateDateTimezones extends Task210901UpdateDateTimezones {

    static final String COLUMN_DATE_TYPE_WITH_TZ_SUPPORT = "datetimeoffset(3)";

    @Override
    public boolean forceRun() {
        return DbConnectionFactory.isMsSql();
    }

    @Override
    boolean updateTable(final String tableName) throws SQLException {
        boolean tableUpdated = false;
        DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        if (tablesToSkip.contains(tableName)){
            return false;
        }
        
        try (Connection conn = DbConnectionFactory.getConnection()) {
            final List<String> droppedConstraints = dropConstraintsIfAny(tableName, dotDatabaseMetaData, conn);
            final ResultSet results = DotDatabaseMetaData.getColumnsMetaData(conn, tableName);
            while (results.next()) {
                final String typeName = results.getString("TYPE_NAME").toLowerCase();

                if (typeName.startsWith("datetime")) {
                    final String columnName = results.getString("COLUMN_NAME");
                    Logger.info(Task220329UpdateDateTimezones.class, "updating " + tableName + "." + columnName + " to " + COLUMN_DATE_TYPE_WITH_TZ_SUPPORT);
                    final String statementString = String.format("ALTER TABLE %s ALTER COLUMN %s %s ", tableName, columnName, COLUMN_DATE_TYPE_WITH_TZ_SUPPORT);
                    Try.of(() -> conn.prepareStatement(statementString).execute()).onFailure(throwable -> Logger.error(
                            Task220329UpdateDateTimezones.class, "error executing: " + statementString, throwable));
                    tableUpdated = true;
                }
            }

            restoreDroppedConstraintsIfAny(tableName, droppedConstraints);

        }
        return tableUpdated;
    }

    private List<String> dropConstraintsIfAny(final String tableName, final DotDatabaseMetaData dotDatabaseMetaData, final Connection conn) throws SQLException {
        if(!tablesToDropConstraints.contains(tableName)){
            return ImmutableList.of();
        }
            final List<String> constraints = Try.of(()-> dotDatabaseMetaData.getConstraints(tableName)).getOrElse(ImmutableList.of());
            for(final String constraintName:constraints) {
                dotDatabaseMetaData.executeDropConstraint(conn, tableName, constraintName);
            }
        return constraints;
    }

    private void restoreDroppedConstraintsIfAny(final String tableName, final List<String> droppedConstraints) {
        if (!droppedConstraints.isEmpty()) {
            restoreConstraintScripts.get(tableName);
        }
    }

    static final List<String> tablesToDropConstraints = ImmutableList.of("sitesearch_audit");
    
    static final List<String> tablesToSkip = ImmutableList.of("calendar_reminder");

    static final Map<String,List<String>> restoreConstraintScripts = ImmutableMap.of("sitesearch_audit",
            ImmutableList.of("ALTER TABLE sitesearch_audit ADD PRIMARY KEY (job_id, fire_date) ")
    );

}
