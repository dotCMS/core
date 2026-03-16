package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.sql.SQLException;

public class Task260316Test implements StartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    /**
     * Adds the custom {@code Usage} portlet to the appropriate Menu Group.
     *
     * @throws DotDataException An error occurred when adding the 'Usage' portlet.
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            final DotConnect dotConnect = new DotConnect();
            dotConnect.executeStatement("ALTER TABLE identifier DROP COLUMN asset_subtype");
        } catch (SQLException exception) {
            throw new DotDataException(exception.getMessage(),exception);
        }
    }
}
