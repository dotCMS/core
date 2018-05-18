package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.SQLException;

/**
 * This upgrade task inserts into DB and enables a new visitor logger
 * @author nollymar
 */
public class Task04370AddVisitorLogger implements StartupTask {

    public boolean forceRun() {
        return true;
    }

    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            DotConnect dc = new DotConnect();
            dc.executeStatement("insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) " +
                    "values ('1','visitor-v3.log','Log Visitor Filter activity on dotCMS.')");
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

}
