package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableSet;
import java.sql.SQLException;
import java.util.Set;

public class Task05390MakeRoomForLongerJobDetail implements StartupTask {

    private static final String ALTER_TABLE_TEMPLATE = "ALTER TABLE %s MODIFY job_data LONGBLOB;";

    @Override
    public boolean forceRun() {
        return DbConnectionFactory.isMySql();
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Logger.debug(this, "Making room for longer job details.");
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        final Set<String> jobDetailTables = ImmutableSet.of("qrtz_excl_job_details","qrtz_job_details","qrtz_triggers","qrtz_excl_triggers");
        final DotConnect dotConnect = new DotConnect();

        for (final String tableName : jobDetailTables) {
            try {
                applyAlterTable(tableName, dotConnect);
            } catch (SQLException e) {
                throw new DotDataException(e);
            }
        }

    }

    private boolean applyAlterTable(final String tableName, final DotConnect dotConnect) throws SQLException {
       final String sqlStatement = String.format(ALTER_TABLE_TEMPLATE,tableName);
       Logger.info(Task05390MakeRoomForLongerJobDetail.class, String.format("Applying `%s` ", sqlStatement));
       return dotConnect.executeStatement(sqlStatement);
    }

}
