package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.SQLException;

/**
 * Adds the nullable {@code default_base_type} column to the {@code folder} table. This column
 * records the folder's Content Drive upload-mode preference (a {@code BaseContentType} name such
 * as {@code DOTASSET}/{@code FILEASSET}, or {@code null} for "no preference"). Additive and
 * backward compatible: existing folders read back {@code null}.
 */
public class Task260720AddDefaultBaseTypeToFolderTable implements StartupTask {

    static final String POSTGRES_SCRIPT = "ALTER TABLE folder ADD COLUMN default_base_type varchar(36) null;";
    static final String MYSQL_SCRIPT = "ALTER TABLE folder ADD default_base_type varchar(36) null;";
    static final String ORACLE_SCRIPT = "ALTER TABLE folder ADD default_base_type varchar2(36) null";
    static final String MSSQL_SCRIPT = "ALTER TABLE folder ADD default_base_type NVARCHAR(36) null;";

    @Override
    public boolean forceRun() {
        return !hasDefaultBaseTypeColumn();
    }

    private boolean hasDefaultBaseTypeColumn() {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        try {
            return databaseMetaData.hasColumn("folder", "default_base_type");
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
