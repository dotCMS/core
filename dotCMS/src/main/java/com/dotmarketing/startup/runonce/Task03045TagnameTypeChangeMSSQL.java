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
 * @author Samuel Mortha
 */
public class Task03045TagnameTypeChangeMSSQL implements StartupTask {

    public boolean forceRun() {
        return true;
    }

    void alterProcedure() throws SQLException {
        if (DbConnectionFactory.isMsSql()) {
        	DotConnect dc = new DotConnect();
        	String alterSql = "alter table tag drop constraint tag_tagname_host;" +
        			"alter table tag alter column tagname nvarchar(255) null;" +
        			"alter table tag add constraint tag_tagname_host unique (tagname, host_id);";
        	dc.executeStatement(alterSql);
        }
    }


    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
        	alterProcedure();
        } catch (Exception ex) {
            throw new DotRuntimeException(ex.getMessage(), ex);
        }
    }


}
