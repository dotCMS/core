package com.dotmarketing.startup.runonce;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;

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

    boolean hasTimeZones() {
        final String type = new DotConnect().setSQL("select udt_name from information_schema.columns where table_name='contentlet' and column_name='date25'").getString("udt_name");
        // if the date fields are no longer with us, this task will still run
        if(type==null) {
            return true;
        }
        return "timestamptz".equals(type );
    }

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


    boolean updateTable(final String tableName) throws SQLException, DotDataException {
        boolean tableUpdated = false;
        try (Connection conn = DbConnectionFactory.getConnection()) {
            final ResultSet results = DotDatabaseMetaData.getColumnsMetaData(conn, tableName);
            while (results.next()) {
                if ("timestamp".equalsIgnoreCase(results.getString("TYPE_NAME"))) {
                    final String columnName = results.getString("COLUMN_NAME");
                    Logger.info(Task210901UpdateDateTimezones.class,
                                    "updating " + tableName + "." + columnName + " to timestamp with timezone");
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

    private int tablesCount;

    public int getTablesCount() {
        return tablesCount;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        
        
        // add the current offset to the content date fields
        if(!hasTimeZones()) {
            updateContentDateFieldsToUTC();
        }
        
        
        
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



    }
    
    @VisibleForTesting
    String selectTimeZone() {
        return new DotConnect().setSQL("select timezoneid from user_ where userid='dotcms.org.default'").getString("timezoneid");

    }
    
    
    
    @VisibleForTesting
    int calculateOffsetSeconds() {
        TimeZone timezone = TimeZone.getTimeZone(selectTimeZone());
        return timezone.getRawOffset()/1000;
    }
    
    
    @VisibleForTesting
    void updateContentDateFieldsToUTC() throws DotDataException {
    
        int offset = calculateOffsetSeconds();

        for(int i=1;i<26;i++) {
            final String sql = "update contentlet set {1} = {1} + interval '{2}' SECOND where {1} is not null"
                            .replace("{1}", "date" + i)
                            .replace("{2}", String.valueOf(offset));

            new DotConnect().setSQL(sql).loadResult();

        }

    }
    
    


}
