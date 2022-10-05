package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * This class updates all postgres "timestamp without timezone" fields
 * to timestamp with timezone fields.  This allows us to store dates and times 
 * with timezone information without forcing us to conver every date/time to
 * UTC
 *
 */
public class Task210901UpdateDateTimezones extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return DbConnectionFactory.isPostgres();
    }

    /**
     * Determines whether the {@code contentlet} table in the current dotCMS schema has the {@code timestamptz} column
     * type set in the 'date' columns or not. That is, if they are already taking the Time Zone into account.
     *
     * @return If the 'date' columns are of type {@code timestamptz}, returns true.
     */
    boolean hasTimeZones() {
        final String type = new DotConnect().setSQL("select udt_name from information_schema.columns where table_name='contentlet' and column_name='date25'").getString("udt_name");
        // if the date fields are no longer with us, this task will still run
        if(type==null) {
            return true;
        }
        return "timestamptz".equals(type );
    }

    /**
     * Returns all table names in the current dotCMS database.
     *
     * @return The list of table names.
     *
     * @throws SQLException An error occurred when accessing the database metadata.
     */
    List<String> findAllTables() throws SQLException {
        final String[] types = {
                "TABLE"};
        final List<String> results = new ArrayList<>();
        try (Connection conn = DbConnectionFactory.getConnection()) {
            final DatabaseMetaData metaData = conn.getMetaData();
            final ResultSet tables = metaData.getTables(null, null, "%", types);
            while (tables.next()) {
                results.add(tables.getString("TABLE_NAME"));
            }
        }
        return results;
    }

    /**
     * Traverses all the columns of the specified table looking for those of type {@code timestamp}. If there's any,
     * they will be transformed into columns of type {@code timestamptz}.
     *
     * @param tableName The name of the table whose columns will be checked.
     *
     * @return If the table had any columns of type {@code timestamp} and they were updated correctly, returns {@code
     * true}. Otherwise, if the table didn't have any or an error occurred, returns {@code false}.
     *
     * @throws SQLException An error occurred when accessing the database metadata.
     */
    boolean updateTable(final String tableName) throws SQLException {
        boolean tableUpdated = false;
        try (Connection conn = DbConnectionFactory.getConnection()) {
            final ResultSet results = DotDatabaseMetaData.getColumnsMetaData(conn, tableName);
            while (results.next()) {
                if ("timestamp".equalsIgnoreCase(results.getString("TYPE_NAME"))) {
                    final String columnName = results.getString("COLUMN_NAME");
                    Logger.info(this, "Updating " + tableName + "." + columnName + " to timestamp with timezone");
                    String statmentString =
                                    "ALTER TABLE \"{0}\" ALTER COLUMN \"{1}\" TYPE TIMESTAMP WITH TIME ZONE USING \"{2}\"";
                    statmentString = statmentString.replace("{0}", tableName).replace("{1}", columnName).replace("{2}",
                                    columnName);
                    conn.prepareStatement(statmentString).execute();
                    tableUpdated = true;
                }
            }
        }
        return tableUpdated;
    }

    protected int tablesCount;

    /**
     * Returns the total amount of dotCMS database tables that have been updated by this upgrade Task.
     *
     * @return The total number of updated tables.
     */
    public int getTablesCount() {
        return tablesCount;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        tablesCount = 0;
        try {
            for (final String table : findAllTables()) {
                if(this.updateTable(table)){
                  tablesCount++;
                }
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
        updateContentDateFieldsToUTC();
    }

    /**
     * Returns the Time Zone that has been selected by the System Administrators via the dotCMS back-end.
     *
     * @return The currently selected Time Zone.
     */
    @VisibleForTesting
    String selectTimeZone() {
        return new DotConnect().setSQL("select timezoneid from user_ where userid='dotcms.org.default'").getString("timezoneid");
    }

    /**
     * Returns the time difference in seconds between the currently selected Time Zone and UTC.
     *
     * @return The time difference in seconds.
     */
    @VisibleForTesting
    int calculateOffsetSeconds() {
        final TimeZone timeZone = TimeZone.getTimeZone(selectTimeZone());
        final int offset = fromMillisToSeconds(timeZone.getRawOffset());
        Logger.info(this, String.format("Time offset from UTC to %s is %s seconds.", timeZone.getDisplayName(),
                offset));
        // For existing non-time zone dates, the UTC offset must be multiplied by -1 in order to correctly add or
        // subtract it from every date, depending on the hemisphere that the dotCMS Time Zone is located in
        return offset * -1;
    }

    /**
     * Updates the value of all 'date' columns in the {@code contentlet} table as well as the {@code mod_date} column.
     *
     * @throws DotDataException An error occurred when updating the values.
     */
    @VisibleForTesting
    void updateContentDateFieldsToUTC() throws DotDataException {
        int offset = calculateOffsetSeconds();
        String sql;
        for(int i=1;i<26;i++) {
            Logger.info(this, "Updating contentlet.date" + i + " column value to UTC time");
            sql = "update contentlet set {1} = {1} + interval '{2}' SECOND where {1} is not null"
                            .replace("{1}", "date" + i)
                            .replace("{2}", String.valueOf(offset));
            new DotConnect().setSQL(sql).loadResult();
        }
        sql = "UPDATE contentlet SET mod_date = mod_date + INTERVAL '" + offset + "' SECOND WHERE mod_date IS NOT NULL";
        new DotConnect().setSQL(sql).loadResult();
    }

    /**
     * Transforms milliseconds into seconds.
     *
     * @param millis The milliseconds being transformed.
     *
     * @return The resulting value in seconds.
     */
    private int fromMillisToSeconds(final int millis) {
        return 0 == millis ? millis : millis / 1000;
    }

}
