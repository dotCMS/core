package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;

public class Task210510UpdateStorageDropMetadataColum extends AbstractJDBCStartupTask {

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
    public String getH2Script() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
