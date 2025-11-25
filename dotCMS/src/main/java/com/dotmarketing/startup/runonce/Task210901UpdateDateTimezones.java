package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.DataSourceAttributes;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DataSourceStrategyProvider;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.URLEncoder;
import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.control.Try;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class updates all postgres "timestamp without timezone" fields
 * to timestamp with timezone fields.  This allows us to store dates and times 
 * with timezone information without forcing us to conver every date/time to
 * UTC
 *
 */
public class Task210901UpdateDateTimezones extends AbstractJDBCStartupTask {

    /** The time offset in seconds between UTC and the Time Zone selected by the CMS Admin in the back-end */
    protected int offsetFromUTCtoTimezone = 0;
    /** The difference in seconds for the Daylight Savings Time, if applicable */
    protected int dstOffset = 0;

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
    boolean updateTable(final String tableName) throws Exception {
        boolean tableUpdated = false;
        try (Connection conn = this.getDbConnection()) {
            final ResultSet results = DotDatabaseMetaData.getColumnsMetaData(conn, tableName);
            while (results.next()) {
                if ("timestamp".equalsIgnoreCase(results.getString("TYPE_NAME"))) {
                    final String columnName = results.getString("COLUMN_NAME");
                    Logger.info(this, "Updating '" + tableName + "." + columnName + "' column to timestamp with timezone");
                    String statementString =
                                    "ALTER TABLE \"{0}\" ALTER COLUMN \"{1}\" TYPE TIMESTAMP WITH TIME ZONE USING \"{2}\"";
                    statementString = statementString.replace("{0}", tableName).replace("{1}", columnName).replace("{2}",
                                    columnName);
                    conn.prepareStatement(statementString).execute();
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
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        tablesCount = 0;
        try {
            final TimeZone defaultTz = TimeZone.getDefault();
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            for (final String table : findAllTables()) {
                if(this.updateTable(table)){
                  tablesCount++;
                }
            }
            TimeZone.setDefault(defaultTz);
            updateDateFieldsToUTC();
        } catch (final Exception e) {
            Logger.error(this.getClass(), e.getMessage());
            throw new DotRuntimeException(e);
        }
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
     * Returns the time difference in seconds between the currently selected Time Zone and UTC. This method will
     * multiply the offset by -1 so that dates can be translated correctly into the expected Time Zone. For example, the
     * offset between UTC and EST is -18000 seconds (-5 hours). So, in order to translate a date and time from UTC into
     * EST, we now need to ADD 18000 seconds to the UTC date, which is why we need to multiply it by -1:
     * {@code -18000 x -1 = 18000}
     * <p>Additionally, if the user-selected Time Zone has Daylight Savings Time, such an offset will be calculated as
     * well, at least in a generic way.</p>
     *
     * @return The time difference in seconds from UTC to the user-selected Time Zone.
     */
    @VisibleForTesting
    int calculateOffsetSeconds() {
        final TimeZone timeZone = TimeZone.getTimeZone(selectTimeZone());
        final int offset = fromMillisToSeconds(timeZone.getRawOffset());
        Logger.info(this, String.format("Time offset from UTC to %s is %s seconds.", timeZone.getDisplayName(),
                offset));
        // For existing non-time zone dates, the UTC offset must be multiplied by -1 in order to correctly add or
        // subtract it from every date, depending on the hemisphere that the dotCMS Time Zone is located in
        offsetFromUTCtoTimezone = offset * -1;
        dstOffset = fromMillisToSeconds(Calendar.getInstance(timeZone, Locale.ENGLISH).get(Calendar.DST_OFFSET));
        if (dstOffset != 0) {
            Logger.info(this, String.format("IMPORTANT! Time Zone '%s' has a DST Offset of %s seconds. Date " +
                                                    "re-adjustment must be applied later.", timeZone.getDisplayName(),
                    dstOffset));
            // If the original Time Zone offset is negative, we need to multiply the DST offset by -1 in order to
            // subtract it from existing dates. If not, we just add it
            dstOffset = offset <= 0 ? dstOffset * -1 : dstOffset;
        }
        return offsetFromUTCtoTimezone;
    }

    /**
     * Updates the values of specific 'date' columns in several dotCMS tables.
     *
     * @throws DotDataException An error occurred when updating date values.
     */
    @VisibleForTesting
    void updateDateFieldsToUTC() throws DotDataException {
        calculateOffsetSeconds();
        applyTimeOffsetToDates(offsetFromUTCtoTimezone);
        if (dstOffset != 0) {
            Logger.info(this, String.format("Re-adjusting dates belonging to DST:"));
            applyTimeOffsetToDates(dstOffset, true);
        }
    }

    private void applyTimeOffsetToDates(final int offsetFromUTCtoTimezone) throws DotDataException {
        applyTimeOffsetToDates(offsetFromUTCtoTimezone, false);
    }

    private void applyTimeOffsetToDates(final int offsetFromUTCtoTimezone, final boolean considerDst) throws DotDataException {
        // Updating all 25 date values in the "contentlet" table
        for (int i = 1; i < 26; i++) {
            updateDateValue("contentlet", "date" + i, offsetFromUTCtoTimezone, considerDst);
        }
        
        if(Config.getBooleanProperty("UPDATE_CONTENT_MOD_DATE_BY_TIMEZONE", false)) {
            // Updating "mod_date" value in the "contentlet" table
            updateDateValue("contentlet", "mod_date", offsetFromUTCtoTimezone, considerDst);
        }
        // Updating date values in the "identifier" table
        updateDateValue("identifier", "syspublish_date", offsetFromUTCtoTimezone, considerDst);
        updateDateValue("identifier", "sysexpire_date", offsetFromUTCtoTimezone, considerDst);
        updateDateValue("identifier", "create_date", offsetFromUTCtoTimezone, considerDst);
    }

    /**
     * Runs the query that updates the value of a specific date with timezone column with the appropriate time offset
     * based on the Time Zone selected by the respective dotCMS Admin.
     *
     * @param table  The name of the dotCMS table containing the specified date column.
     * @param column The date column that will be updated.
     * @param offset The time offset in seconds from the UTC Time Zone to the user-selected Time Zone.
     *
     * @throws DotDataException An error occurred when running the SQL query.
     */
    private void updateDateValue(final String table, final String column, final int offset, final boolean considerDst) throws DotDataException {

        Logger.info(this, String.format("Adjusting current '%s.%s' values by %s seconds", table, column, offset));
        String sql;
        if (considerDst) {//we apply DST from March 13th to November 5th
            sql = ("UPDATE {1} SET {2} = {2} + INTERVAL '{3}' SECOND WHERE {2} IS NOT NULL AND " +
                                        "(DATE_PART('month',{2}) = '4' OR DATE_PART('month',{2}) = '5' OR " +
                                        "DATE_PART('month',{2}) = '6' OR DATE_PART('month',{2}) = '7' OR " +
                                        "DATE_PART('month',{2}) = '8' OR DATE_PART('month',{2}) = '9' OR " +
                                        "DATE_PART('month',{2}) = '10')")
                                       .replace("{1}", table)
                                       .replace("{2}", column)
                                       .replace("{3}", String.valueOf(offset));
            new DotConnect().setSQL(sql).loadResult();
            sql = ("UPDATE {1} SET {2} = {2} + INTERVAL '{3}' SECOND WHERE {2} IS NOT NULL AND " +
                           "DATE_PART('month',{2}) = '3' AND DATE_PART('day',{2}) BETWEEN '13' and '31'")
                          .replace("{1}", table)
                          .replace("{2}", column)
                          .replace("{3}", String.valueOf(offset));
            new DotConnect().setSQL(sql).loadResult();
            sql = ("UPDATE {1} SET {2} = {2} + INTERVAL '{3}' SECOND WHERE {2} IS NOT NULL AND " +
                           "DATE_PART('month',{2}) = '11' AND DATE_PART('day',{2}) BETWEEN '01' and '05'")
                          .replace("{1}", table)
                          .replace("{2}", column)
                          .replace("{3}", String.valueOf(offset));
            new DotConnect().setSQL(sql).loadResult();
        } else {
            sql = "UPDATE {1} SET {2} = {2} + INTERVAL '{3}' SECOND WHERE {2} IS NOT NULL"
                                       .replace("{1}", table)
                                       .replace("{2}", column)
                                       .replace("{3}", String.valueOf(offset));
            new DotConnect().setSQL(sql).loadResult();
        }
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

    /**
     * Gets the current datasource to get DB connection info. We need this in order to force dotCMS into creating a
     * DataSource object using the UTC Time Zone.
     *
     * @return DB connection info
     */
    private Connection getDbConnection() {
        try {
            Class dbDriver = Class.forName("org.postgresql.Driver");
            final HikariDataSource hds = (HikariDataSource) DbConnectionFactory.getDataSource();
            return DriverManager.getConnection(hds.getJdbcUrl(), hds.getUsername(), hds.getPassword());
        } catch (final Exception e) {
            Logger.error(this.getClass(), String.format("DataSource object could not be retrieved: %s",
                    e.getMessage()), e);
            throw new DotRuntimeException(e);
        }
    }

}
