package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.sql.SQLException;

/**
 * This task updates the 'host' db column of the 'structure' table to System Host's identifier for 'fixed' records
 *
 */
public class Task05030UpdateSystemContentTypesHost implements StartupTask {

    private static final String UPDATE_HOST_ON_FIXED_TYPES = "UPDATE structure SET host = 'SYSTEM_HOST' " +
            "WHERE fixed = " + DbConnectionFactory.getDBTrue() + " AND velocity_var_name <> 'Comments'";

    private static final String REMOVE_FIXED_ON_COMMENTS_TYPE = "UPDATE structure SET " +
            "fixed = " + DbConnectionFactory.getDBFalse() + " WHERE velocity_var_name = 'Comments'";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            final DotConnect dotConnect = new DotConnect();

            dotConnect.setSQL(UPDATE_HOST_ON_FIXED_TYPES);
            dotConnect.loadResult();

            dotConnect.setSQL(REMOVE_FIXED_ON_COMMENTS_TYPE);
            dotConnect.loadResult();
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

}
