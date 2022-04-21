package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.sql.SQLException;

public class Task220404RemoveCalendarReminder implements StartupTask {

    @Override
    public boolean forceRun() {
        DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
        return Try.of(()->databaseMetaData.tableExists(DbConnectionFactory.getConnection(),"calendar_reminder")).getOrElse(false);
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            new DotConnect().executeStatement("DROP TABLE calendar_reminder");
        } catch (SQLException e) {
            Logger.error(Task220404RemoveCalendarReminder.class, "Error while attempting to remove table calendar_reminder.",e);
        }
    }
}
