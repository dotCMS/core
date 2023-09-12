package com.dotmarketing.startup.runonce;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.sql.SQLException;

/**
 * This class alter the locked_by column in four different tables.
 * The locked_by column in some coses has a length of 36. With this class
 * we are increasing that length to 100.
 */
public class Task230426AlterVarcharLengthOfLockedByCol implements StartupTask {
    private void alterTables() throws SQLException {
        final DotConnect dc = new DotConnect();
        if (DbConnectionFactory.isMsSql()) {
            dc.executeStatement("drop index cvi_locked_by_index on contentlet_version_info");
            dc.executeStatement("alter table contentlet_version_info drop CONSTRAINT FK_con_ver_lockedby");
            dc.executeStatement("alter table contentlet_version_info alter column locked_by nvarchar (100)");
            dc.executeStatement("create index cvi_locked_by_index on contentlet_version_info (locked_by)");
            dc.executeStatement("alter table contentlet_version_info add constraint FK_con_ver_lockedby foreign key (locked_by) references user_(userid)");
            dc.executeStatement("alter table container_version_info alter column locked_by nvarchar (100)");
            dc.executeStatement("alter table template_version_info alter column locked_by nvarchar (100)");
            dc.executeStatement("alter table link_version_info alter column locked_by nvarchar (100)");
        }else {
            dc.executeStatement("alter table contentlet_version_info alter column locked_by type varchar (100)");
            dc.executeStatement("alter table container_version_info alter column locked_by type varchar (100)");
            dc.executeStatement("alter table template_version_info alter column locked_by type varchar (100)");
            dc.executeStatement("alter table link_version_info alter column locked_by type varchar (100)");
        }
    }
    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
            alterTables();
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(),e);
        }
    }



}