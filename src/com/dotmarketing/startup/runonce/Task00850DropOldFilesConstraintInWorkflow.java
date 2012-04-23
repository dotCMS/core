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
public class Task00850DropOldFilesConstraintInWorkflow implements StartupTask {

    public boolean forceRun() {
        return true;
    }

    void dropConstraint() throws SQLException {
        DotConnect dc = new DotConnect();
        String alterSql = "";
        if (DbConnectionFactory.isMySql())
        	alterSql = "alter table workflowtask_files drop foreign key FK_task_file_inode ";
        else
        	alterSql = "alter table workflowtask_files drop constraint FK_task_file_inode ";

        dc.executeStatement(alterSql);

    }


    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
        	dropConstraint();
        } catch (Exception ex) {
            throw new DotRuntimeException(ex.getMessage(), ex);
        }
    }


}
