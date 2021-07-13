package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;

/**
 * Upgrade task used to add columns `icon` and `sort_order` in `structure` table if needed
 */
public class Task210702UpdateStructureTable extends AbstractJDBCStartupTask {

    private final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();

    @Override
    public boolean forceRun() {
        try {
            return !dotDatabaseMetaData.hasColumn("structure", "icon") ||
                    !dotDatabaseMetaData.hasColumn("structure", "sort_order") ;
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    /**
     * Returns the upgrade SQL query for PostgreSQL.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getPostgresScript() {
        String query = "";
        try {
            if(!dotDatabaseMetaData.hasColumn("structure", "icon")) {
                query += "ALTER TABLE structure ADD icon varchar(255);";
            }
            if(!dotDatabaseMetaData.hasColumn("structure", "sort_order")) {
                query += "ALTER TABLE structure ADD sort_order int4;";
            }
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
        }
        return query;
    }

    /**
     * Returns the upgrade SQL query for MySQL.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getMySQLScript() {
        String query = "";
        try {
            if(!dotDatabaseMetaData.hasColumn("structure", "icon")) {
                query += "ALTER TABLE structure ADD icon varchar(255);";
            }
            if(!dotDatabaseMetaData.hasColumn("structure", "sort_order")) {
                query += "ALTER TABLE structure ADD sort_order integer;";
            }
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
        }
        return query;
    }

    /**
     * Returns the upgrade SQL query for Oracle.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getOracleScript() {
        String query = "";
        try {
            if(!dotDatabaseMetaData.hasColumn("structure", "icon")) {
                query += "ALTER TABLE structure ADD icon varchar2(255);";
            }
            if(!dotDatabaseMetaData.hasColumn("structure", "sort_order")) {
                query += "ALTER TABLE structure ADD sort_order number(10,0);";
            }
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
        }
        return query;
    }

    /**
     * Returns the upgrade SQL query for Microsoft SQL Server.
     *
     * @return The SQL statement(s).
     */
    @Override
    public String getMSSQLScript() {
        String query = "";
        try {
            if(!dotDatabaseMetaData.hasColumn("structure", "icon")) {
                query += "ALTER TABLE structure ADD icon NVARCHAR(255) null;";
            }
            if(!dotDatabaseMetaData.hasColumn("structure", "sort_order")) {
                query += "ALTER TABLE structure ADD sort_order int null;";
            }
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
        }
        return query;
    }

    /**
     * Returns the list of database tables whose keys, indexes, and constraints will be dropped in
     * order to perform intensive changes in their columns.
     * <p>
     * <b>IMPORTANT: The order in which the tables are added to the list
     * matters. The reason is that objects of a given table might need to be dropped first before
     * another table. Read the log errors thoroughly when troubleshooting this.</b>
     */
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
