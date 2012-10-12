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
public class Task00945TemplateThemes implements StartupTask {

    public boolean forceRun() {
        return true;
    }

    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
        	DotConnect dc=new DotConnect();

        	if(DbConnectionFactory.isOracle()) {
        		dc.executeStatement("ALTER TABLE template ADD theme varchar2(255)");
        	} else {
        		dc.executeStatement("ALTER TABLE template ADD theme varchar(255)");
        	}
        } catch (Exception ex) {
            throw new DotRuntimeException(ex.getMessage(), ex);
        }
    }


}
