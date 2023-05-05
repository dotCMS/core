package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.sql.Connection;
import java.sql.SQLException;

public class Task210719CleanUpTitleField implements StartupTask {
    @Override
    public boolean forceRun() {
        return true;
    }

    public void executeUpgrade()  {

        if(DbConnectionFactory.isPostgres()) {
            executePgUpgrade();
        }

        if(DbConnectionFactory.isMsSql()) {
            executeSQLServerUpgrade();
        }
    }

    public void executePgUpgrade()  {

        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()){

            new DotConnect().executeStatement("ALTER TABLE contentlet drop column title", conn);
            new DotConnect().executeStatement("ALTER TABLE contentlet add column title varchar(255)", conn);

        } catch (SQLException exception) {
            throw new DotRuntimeException(exception);
        }
    }


    public void executeSQLServerUpgrade() {

        try {
             new DotConnect()
                    .executeStatement("update contentlet set title=null");
        } catch (SQLException exception) {
            throw new DotRuntimeException(exception);
        }
    }
}
