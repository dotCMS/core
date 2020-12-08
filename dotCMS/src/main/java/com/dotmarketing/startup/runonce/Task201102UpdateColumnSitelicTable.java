package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Task used to create `startup_time` column at sitelic table.
 *
 * @author victor
 */
public class Task201102UpdateColumnSitelicTable implements StartupTask {
    private final static String POSTGRES_COLUMN_TYPE = "BIGINT";
    private final static String MYSQL_COLUMN_TYPE = "BIGINT";
    private final static String ORACLE_COLUMN_TYPE = "NUMBER(19,0)";
    private final static String MSSQL_COLUMN_TYPE = "BIGINT";

    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().hasColumn("sitelic", "startup_time");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final Optional<String> type = getColumnType();
        if (!type.isPresent()) {
            return;
        }

        new DotConnect()
                .setSQL(String.format("ALTER TABLE sitelic ADD COLUMN startup_time %s", type.get()))
                .loadObjectResults();
    }

    private static Optional<String> getColumnType() {
        final String type;
        if (DbConnectionFactory.isPostgres()) {
            type = POSTGRES_COLUMN_TYPE;
        } else if (DbConnectionFactory.isMySql()) {
            type = MYSQL_COLUMN_TYPE;
        } else if (DbConnectionFactory.isOracle()) {
            type = ORACLE_COLUMN_TYPE;
        } else if (DbConnectionFactory.isMsSql()) {
            type = MSSQL_COLUMN_TYPE;
        } else {
            type = null;
        }

        return Optional.ofNullable(type);
    }
}
