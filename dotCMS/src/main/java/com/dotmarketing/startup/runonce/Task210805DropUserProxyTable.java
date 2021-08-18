package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Task used to remove user_proxy table from database
 */
public class Task210805DropUserProxyTable implements StartupTask {

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        //remove user_proxy table
        try {
            new DotConnect().executeStatement("drop table USER_PROXY");
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public boolean forceRun() {
        try {
            return new DotDatabaseMetaData().tableExists(
                    DbConnectionFactory.getConnection(), "user_proxy");
        } catch (SQLException e) {
            return Boolean.FALSE;
        }
    }
}
