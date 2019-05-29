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

    private static final String MY_SQL_CHECK_VIRTUAL_COLUMN = "SELECT * FROM information_schema.columns WHERE table_schema = ? AND table_name = 'identifier' AND column_name = 'full_path_lc';";
    private static final String ORACLE_CHECK_VIRTUAL_COLUMN = "SELECT * FROM user_tab_cols WHERE upper(table_name) = 'IDENTIFIER' AND upper(column_name) = 'FULL_PATH_LC'";
    private static final String MS_SQL_CHECK_VIRTUAL_COLUMN = "SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'identifier' AND COLUMN_NAME = 'full_path_lc'";

    private static final String MSSQL_ADD_VIRTUAL_COLUMN = "ALTER TABLE identifier ADD full_path_lc  as CASE WHEN parent_path = 'System Host' THEN '/' ELSE LOWER(CONCAT(parent_path, asset_name)) END";
    private static final String ORACLE_ADD_VIRTUAL_COLUMN = "ALTER TABLE identifier ADD full_path_lc as ( CASE WHEN parent_path = 'System folder' THEN '/' ELSE  lower(concat(parent_path, asset_name)) END)";
    private static final String POSTGRES_CREATE_CREATE_VIRTUAL_COLUMN_FUNCTION =
            "CREATE OR REPLACE FUNCTION full_path_lc(identifier) RETURNS text\n"
                    + "    AS ' SELECT CASE WHEN $1.parent_path = ''/System folder'' then ''/'' else LOWER($1.parent_path || $1.asset_name) end; '\n"
                    + "LANGUAGE SQL;\n";
    private static final String MY_SQL_ADD_VIRTUAL_COLUMN = "ALTER TABLE identifier add column full_path_lc varchar(510) as ( IF(parent_path = 'System Folder', '/', lower(concat(parent_path, asset_name)) ))";



    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            try {
                DbConnectionFactory.getConnection().setAutoCommit(true);
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }

            final Connection conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            try {
                final DotConnect dotConnect = new DotConnect();
                if(!checkColumnExists(dotConnect, conn)){
                   addVirtualColumn(dotConnect);
                }
            } finally {
                conn.setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    private boolean checkColumnExists(final DotConnect dotConnect, final Connection conn) throws DotDataException, DotRuntimeException, SQLException {
        boolean exists = false;
        //We do not include postgres here, since for postgres we use a function.
        if (DbConnectionFactory.isOracle()) {
            exists = !dotConnect.setSQL(ORACLE_CHECK_VIRTUAL_COLUMN).getResults().isEmpty();
        }
        if (DbConnectionFactory.isMySql()) {
            exists = !dotConnect.setSQL(MY_SQL_CHECK_VIRTUAL_COLUMN).addParam(conn.getCatalog()).getResults().isEmpty();
        }
        if (DbConnectionFactory.isMsSql()) {
            exists = !dotConnect.setSQL(MS_SQL_CHECK_VIRTUAL_COLUMN).getResults().isEmpty();
        }
        return exists;
    }

    private void addVirtualColumn(final DotConnect dotConnect) throws SQLException {
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
