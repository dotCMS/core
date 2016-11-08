package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task03725AddMissingRolesIntegrityConstraint implements StartupTask {

    private void addMissingRolesIntegrityConstraint(DotConnect dc) throws SQLException, DotDataException {
        if(DbConnectionFactory.isMySql()) {
        	dc.executeStatement("alter table cms_role add constraint cms_role_name_db_fqn unique (db_fqn(1000));");
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
            DotConnect dc=new DotConnect();
            addMissingRolesIntegrityConstraint(dc);
        } catch (SQLException e) {
            throw new DotRuntimeException(e.getMessage(),e);
        }

    }

    @Override
    public boolean forceRun() {
        return true;
    }

}
