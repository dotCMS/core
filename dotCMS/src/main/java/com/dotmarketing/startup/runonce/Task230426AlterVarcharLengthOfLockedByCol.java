package com.dotmarketing.startup.runonce;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.Map;

/**
 * This class alter the locked_by column in four different tables.
 * The locked_by column in some coses has a length of 36. With this class
 * we are increasing that length to 100.
 */
public class Task230426AlterVarcharLengthOfLockedByCol implements StartupTask {
    private void alterTables() throws SQLException {
        final DotConnect dc = new DotConnect();
        dc.executeStatement("alter table contentlet_version_info alter column locked_by type varchar (100)");
        dc.executeStatement("alter table container_version_info alter column locked_by type varchar (100)");
        dc.executeStatement("alter table template_version_info alter column locked_by type varchar (100)");
        dc.executeStatement("alter table link_version_info alter column locked_by type varchar (100)");
    }
    @Override
    public boolean forceRun() {
        return !(new DotDatabaseMetaData().isColumnLengthExpected("contentlet_version_info",
                "locked_by","100"));
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            alterTables();
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(),e);
        }
    }



}
