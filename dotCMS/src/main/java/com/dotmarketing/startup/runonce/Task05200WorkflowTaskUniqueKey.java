package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;

public class Task05200WorkflowTaskUniqueKey implements StartupTask {

   private final static String TABLE_NAME = "workflow_task";

   private final static String CONSTRAINT_NAME = "unique_workflow_task";

   private final static String ADD_CONSTRAINT_SQL = "ALTER TABLE workflow_task ADD CONSTRAINT unique_workflow_task UNIQUE (webasset,language_id)";

    @Override
    public boolean forceRun() {
        try {
           return new DotDatabaseMetaData().getConstraints(TABLE_NAME).stream().map(String::toLowerCase).noneMatch(s -> s.equals(CONSTRAINT_NAME));
        } catch (DotDataException e) {
           throw  new DotRuntimeException(e);
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Logger.debug(this,
                String.format("Upgrading workflow_task table adding `%s` constraint", CONSTRAINT_NAME));
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        final DotConnect dotConnect = new DotConnect();
        try {
            dotConnect.executeStatement(ADD_CONSTRAINT_SQL);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }
}
