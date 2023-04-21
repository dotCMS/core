package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.sql.SQLException;

public class Task230420AlterVarcharLengthOfLockedByCol implements StartupTask {
    private void alterTables() throws SQLException {
        DotConnect dc = new DotConnect();
        dc.executeStatement("alter table contentlet_version_info alter column locked_by type varchar (100);");
        dc.executeStatement("alter table container_version_info alter column locked_by type varchar (100);");
        dc.executeStatement("alter table template_version_info alter column locked_by type varchar (100);");
        dc.executeStatement("alter table link_version_info alter column locked_by type varchar (100);");
    }
    @Override
    public boolean forceRun() {
        return true;
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
