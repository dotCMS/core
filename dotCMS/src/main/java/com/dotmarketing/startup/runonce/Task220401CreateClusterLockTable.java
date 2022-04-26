package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 * Upgrade task used to create tables: `shedlock`
 * @author jsanca
 */
public class Task220401CreateClusterLockTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        try {
            final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
            return !databaseMetaData.tableExists(DbConnectionFactory.getConnection(), "shedlock");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public String getPostgresScript() {
        return "CREATE TABLE shedlock(name VARCHAR(64) NOT NULL, lock_until timestamptz NOT NULL,\n" +
                "                      locked_at timestamptz NOT NULL, locked_by VARCHAR(255) NOT NULL, PRIMARY KEY (name));";
    }

    @Override
    public String getMySQLScript() {
        return null;
    }

    @Override
    public String getOracleScript() {
        return null;
    }

    @Override
    public String getMSSQLScript() {
        return "CREATE TABLE shedlock(name VARCHAR(64) NOT NULL, lock_until datetime NOT NULL,\n" +
                "                      locked_at datetime NOT NULL, locked_by VARCHAR(255) NOT NULL, PRIMARY KEY (name));\n";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
