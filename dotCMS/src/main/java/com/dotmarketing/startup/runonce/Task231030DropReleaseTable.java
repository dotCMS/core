package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.SQLException;

/**
 * Task used to remove Release_ table from database
 */
public class Task231030DropReleaseTable implements StartupTask {

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        //remove Release_ table
        try {
            new DotConnect().executeStatement("drop table Release_");
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public boolean forceRun() {
        try {
            return new DotDatabaseMetaData().tableExists(
                    DbConnectionFactory.getConnection(), "Release_");
        } catch (SQLException e) {
            return Boolean.FALSE;
        }
    }
}
