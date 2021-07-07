package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;

/**
 * Apparently the table storage went out on the last releases with an old column named metadata which is no longer used
 */
public class Task210510UpdateStorageTableDropMetadataColumn extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        try {
            return new DotDatabaseMetaData().hasColumn("storage", "metadata") ;
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public String getPostgresScript() {
        return "ALTER TABLE storage DROP COLUMN metadata;";
    }

    @Override
    public String getMySQLScript() {
        return "ALTER TABLE storage DROP COLUMN metadata;";
    }

    @Override
    public String getOracleScript() {
        return "ALTER TABLE storage DROP COLUMN metadata;";
    }

    @Override
    public String getMSSQLScript() {
        return "ALTER TABLE storage DROP COLUMN metadata;";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
