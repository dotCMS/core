package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class updates all MS-SQL "datetime without timezone" fields
 * to timestamp with timezone fields.  This allows us to store dates and times 
 * with timezone information without forcing us to convert every date/time to
 * UTC
 *
 */
public class Task220424UpdateDateTimezones extends Task210901UpdateDateTimezones {

    static final String COLUMN_DATE_TYPE_WITH_TZ_SUPPORT = "datetimeoffset";

    @Override
    public boolean forceRun() {
        return DbConnectionFactory.isMsSql();
    }

    @Override
    boolean updateTable(final String tableName) throws SQLException {
        boolean tableUpdated = false;
        try (Connection conn = DbConnectionFactory.getConnection()) {
            final ResultSet results = DotDatabaseMetaData.getColumnsMetaData(conn, tableName);
            while (results.next()) {
                final String typeName = results.getString("TYPE_NAME").toLowerCase();
                if (typeName.startsWith("datetime")) {
                    final String columnName = results.getString("COLUMN_NAME");
                    Logger.info(Task220424UpdateDateTimezones.class, "updating " + tableName + "." + columnName + " to "+COLUMN_DATE_TYPE_WITH_TZ_SUPPORT);
                    final String statementString = String.format("ALTER TABLE %s ALTER COLUMN %s %s ",tableName, columnName,
                            COLUMN_DATE_TYPE_WITH_TZ_SUPPORT);
                    conn.prepareStatement(statementString).execute();
                    tableUpdated = true;
                }
            }
        }
        return tableUpdated;
    }

}
