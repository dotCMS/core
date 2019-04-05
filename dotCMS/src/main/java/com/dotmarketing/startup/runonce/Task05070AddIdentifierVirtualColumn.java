package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.Connection;
import java.sql.SQLException;

public class Task05070AddIdentifierVirtualColumn implements StartupTask {


    private static final String MSSQL_ADD_VIRTUAL_COLUMN = "ALTER TABLE identifier ADD full_path_lc  as CASE WHEN parent_path = 'System Host' THEN '/' ELSE LOWER(CONCAT(parent_path, asset_name)) END;";
    private static final String ORACLE_ADD_VIRTUAL_COLUMN = "ALTER TABLE identifier ADD full_path_lc as ( CASE WHEN parent_path = 'System folder' THEN '/' ELSE  lower(concat(parent_path, asset_name)) END)";
    private static final String POSTGRES_CREATE_CREATE_VIRTUAL_COLUMN_FUNCTION =
            "CREATE OR REPLACE FUNCTION full_path_lc(identifier) RETURNS text\n"
                    + "    AS ' SELECT CASE WHEN $1.parent_path = ''/System folder'' then ''/'' else LOWER($1.parent_path || $1.asset_name) end; '\n"
                    + "LANGUAGE SQL;\n";
    private static final String MY_SQL_ADD_VIRTUAL_COLUMN = "ALTER TABLE identifier add column full_path_lc varchar(510) as ( IF(parent_path = 'System Folder', '/', concat(parent_path, asset_name)));";

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            final Connection conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            try {
                addVirtualColumn();
            } finally {
                conn.setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    private void addVirtualColumn() throws SQLException {
        final DotConnect dotConnect = new DotConnect();
        if (DbConnectionFactory.isPostgres()) {
            dotConnect.executeStatement(POSTGRES_CREATE_CREATE_VIRTUAL_COLUMN_FUNCTION);
        }
        if (DbConnectionFactory.isOracle()) {
            dotConnect.executeStatement(ORACLE_ADD_VIRTUAL_COLUMN);
        }
        if (DbConnectionFactory.isMySql()) {
            dotConnect.executeStatement(MY_SQL_ADD_VIRTUAL_COLUMN);
        }
        if (DbConnectionFactory.isMsSql()) {
            dotConnect.executeStatement(MSSQL_ADD_VIRTUAL_COLUMN);
        }
    }

}
