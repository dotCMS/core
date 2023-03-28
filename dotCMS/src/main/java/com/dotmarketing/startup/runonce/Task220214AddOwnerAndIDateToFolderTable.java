package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.SQLException;

public class Task220214AddOwnerAndIDateToFolderTable implements StartupTask {

    static final String POSTGRES_SCRIPT = "ALTER TABLE folder ADD COLUMN owner varchar(255) null;"
            + "ALTER TABLE folder ADD COLUMN idate timestamptz null;";
    static final String MYSQL_SCRIPT = "ALTER TABLE folder ADD owner varchar(255) null;"
            + "ALTER TABLE folder ADD idate datetime null;";
    static final String ORACLE_SCRIPT = "ALTER TABLE folder ADD owner varchar2(255) null;"
            + "ALTER TABLE folder ADD idate date null";
    static final String MSSQL_SCRIPT = "ALTER TABLE folder ADD owner NVARCHAR(255) null;"
            + "ALTER TABLE folder ADD idate datetime null;";

    @Override
    public boolean forceRun() {
        return !hasOwnerField();
    }

    private boolean hasOwnerField() {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        try {
            return databaseMetaData.hasColumn("folder", "owner");
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        final DotConnect dotConnect = new DotConnect();
        final String columnScript = getAddColumnScript();
        dotConnect.setSQL(columnScript);
        dotConnect.loadResult();

    }

    private String getAddColumnScript() {
        String sqlScript = null;
        if (DbConnectionFactory.isPostgres()) {
            sqlScript = POSTGRES_SCRIPT;
        } else if (DbConnectionFactory.isMySql()) {
            sqlScript = MYSQL_SCRIPT;
        } else if (DbConnectionFactory.isOracle()) {
            sqlScript = ORACLE_SCRIPT;
        } else if (DbConnectionFactory.isMsSql()) {
            sqlScript = MSSQL_SCRIPT;
        }
        return sqlScript;
    }
}
