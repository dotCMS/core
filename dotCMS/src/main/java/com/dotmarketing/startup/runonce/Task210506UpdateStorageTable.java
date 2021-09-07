package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;

/**
 * Upgrade task used to add column `hash_ref` in `storage` table if needed
 */
public class Task210506UpdateStorageTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().hasColumn("storage", "hash_ref") ;
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public String getPostgresScript() {
        return "ALTER TABLE storage ADD COLUMN hash_ref varchar(64);";
    }

    @Override
    public String getMySQLScript() {
        return "ALTER TABLE storage ADD hash_ref varchar(64);";
    }

    @Override
    public String getOracleScript() {
        return "ALTER TABLE storage ADD hash_ref varchar(64);";
    }

    @Override
    public String getMSSQLScript() {
        return "ALTER TABLE storage ADD hash_ref varchar(64);";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
