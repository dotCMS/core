package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;

/**
 * We're adding a new column to the stable storage which basically is a nullable pointer to another sha in the storage
 * It's would be useful from the trouble shooting - support perspective.
 */
public class Task210303AddHashRefColumnStorageTable implements StartupTask {

    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().hasColumn("storage", "hash_ref");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        new DotConnect()
                .setSQL("ALTER TABLE storage ADD hash_ref varchar(64)")
                .loadObjectResults();

        try {
            DbConnectionFactory.getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }
}
