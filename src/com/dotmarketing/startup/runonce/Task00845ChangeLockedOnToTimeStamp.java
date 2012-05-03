package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 *
 *
 * @author Daniel Silva
 */
public class Task00845ChangeLockedOnToTimeStamp implements StartupTask {

    public boolean forceRun() {
        return true;
    }

    void changeField() throws SQLException {
        DotConnect dc = new DotConnect();
        String alterSql = "";
        if (DbConnectionFactory.isOracle())
        	alterSql = "alter table contentlet_version_info modify locked_on TIMESTAMP ";
        else if (DbConnectionFactory.isMsSql())
        	alterSql = "alter table contentlet_version_info alter column locked_on datetime  ";
        else if (DbConnectionFactory.isMySql())
        	alterSql = "alter table contentlet_version_info modify column locked_on TIMESTAMP ";
        else if (DbConnectionFactory.isPostgres())
        	alterSql = "alter table contentlet_version_info alter column locked_on TYPE TIMESTAMP ";

        dc.executeStatement(alterSql);

    }


    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
        	changeField();
        } catch (Exception ex) {
            throw new DotRuntimeException(ex.getMessage(), ex);
        }
    }


}
