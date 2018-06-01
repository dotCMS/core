package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.SQLException;

/**
 * This upgrade task updates the fields type_ and street of the company table, those columns
 * are actually for the Primary and Secondary Color used in dotcms backend.
 */
public class Task04375UpdateColors implements StartupTask {

    private final String UPDATE_PRIMARY_COLOR_QUERY = "update company set type_ = ?";
    private final String UPDATE_SECONDARY_COLOR_QUERY = "update company set street = ?";
    protected final String PRIMARY_COLOR = "#C336E5";
    protected final String SECONDARY_COLOR = "#54428E";

    public boolean forceRun() {
        return true;
    }

    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            DotConnect dc = new DotConnect();

            dc.setSQL(UPDATE_PRIMARY_COLOR_QUERY);
            dc.addParam(PRIMARY_COLOR);
            dc.loadResult();

            dc.setSQL(UPDATE_SECONDARY_COLOR_QUERY);
            dc.addParam(SECONDARY_COLOR);
            dc.loadResult();
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }
}
