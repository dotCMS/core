package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link StartupTask} to create a running_ids field in the experiment table
 */
public class Task230630CreateRunningIdsExperimentField implements StartupTask  {

    @Override
    public boolean forceRun() {
        try {
            return !isExistsRunningIds();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isExistsRunningIds() throws SQLException {
        final Connection connection = DbConnectionFactory.getConnection();

        boolean existsRunningIds = false;
        final ResultSet resultSet = DotDatabaseMetaData.getColumnsMetaData(connection, "experiment");
        while(resultSet.next()){
            final String columnName = resultSet.getString("COLUMN_NAME");

            if (columnName.equals("running_ids")) {
                existsRunningIds = true;
                break;
            }
        }
        return existsRunningIds;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        if (forceRun()) {
            final DotConnect dotConnect = new DotConnect();
            try {
                dotConnect.executeStatement(getStatements());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getStatements() throws  DotRuntimeException {
        return "ALTER TABLE experiment ADD running_ids jsonb";
    }
}
