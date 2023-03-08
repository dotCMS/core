package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.Optional;

public class Task211101AddContentletAsJsonColumn implements StartupTask {

    private final static String POSTGRES_COLUMN_TYPE = "JSONB";
    private final static String MYSQL_COLUMN_TYPE = "TEXT";
    private final static String ORACLE_COLUMN_TYPE = "NCLOB";
    private final static String MSSQL_COLUMN_TYPE = "NVARCHAR(MAX)";

    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().hasColumn("contentlet", "contentlet_as_json");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final Optional<String> type = getColumnType();
        if (!type.isPresent()) {
            return;
        }

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        new DotConnect()
                .setSQL(String.format("ALTER TABLE contentlet ADD contentlet_as_json %s", type.get()))
                .loadObjectResults();

        try {
            DbConnectionFactory.getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
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
