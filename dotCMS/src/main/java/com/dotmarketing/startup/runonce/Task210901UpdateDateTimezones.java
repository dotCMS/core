package com.dotmarketing.startup.runonce;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

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



    boolean updateTable(final String tableName) throws SQLException {

        try (Connection conn = DbConnectionFactory.getConnection()) {
            final ResultSet results = new DotDatabaseMetaData().getColumnsMetaData(conn, tableName);
            while (results.next()) {
                if ("timestamp".equals(results.getString("TYPE_NAME"))) {
                    final String columnName = results.getString("COLUMN_NAME");
                    Logger.info(Task210901UpdateDateTimezones.class,
                                    "updating " + tableName + "." + columnName + " to timestamp with timezone");
                    String statmentString =
                                    "ALTER TABLE \"{0}\" ALTER COLUMN \"{1}\" TYPE TIMESTAMP WITH TIME ZONE USING \"{2}\"";
                    statmentString = statmentString.replace("{0}", tableName).replace("{1}", columnName).replace("{2}",
                                    columnName);
                    conn.prepareStatement(statmentString).execute();
                }
            }
        }
        return true;


    }



    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        try {
            for (final String table : findAllTables()) {
                this.updateTable(table);
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }



    }



    @Override
    protected List<String> getTablesToDropConstraints() {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public String getPostgresScript() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMySQLScript() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOracleScript() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMSSQLScript() {
        // TODO Auto-generated method stub
        return null;
    }
}
