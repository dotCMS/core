package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.Connection;
import java.sql.SQLException;

public class Task05080RecreateIdentifierIndex implements StartupTask {

    private static final String POSTGRES_DROP_INDEX = "DROP INDEX IF EXISTS idx_ident_uniq_asset_name CASCADE";
    private static final String ORACLE_DROP_INDEX = "DROP INDEX idx_ident_uniq_asset_name";
    private static final String MSSQL_DROP_INDEX = "DROP INDEX idx_ident_uniq_asset_name ON identifier";
    private static final String MYSQL_DROP_INDEX = "DROP INDEX idx_ident_uniq_asset_name ON identifier";

    private static final String POSTGRES_CREATE_INDEX = "CREATE UNIQUE INDEX idx_ident_uniq_asset_name on identifier (full_path_lc(identifier),host_inode)";
    private static final String ORACLE_CREATE_INDEX = "CREATE UNIQUE INDEX idx_ident_uniq_asset_name on identifier (full_path_lc,host_inode)";
    private static final String MSSQL_CREATE_INDEX = "CREATE UNIQUE INDEX idx_ident_uniq_asset_name on identifier (full_path_lc,host_inode)";
    private static final String MYSQL_CREATE_INDEX = "CREATE UNIQUE INDEX idx_ident_uniq_asset_name on identifier (full_path_lc,host_inode)";


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
            
            dropAndRecreateIndex();
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    private void dropAndRecreateIndex() throws SQLException {
        final DotConnect dotConnect = new DotConnect();
        try{
           attemptDropIndex(dotConnect);
        }catch (SQLException ex){
           // index did not exist.
            Logger.warn(getClass(), " An exception occurred while trying to drop the index `idx_ident_uniq_asset_name` if the index did not exist this can be safely ignore.");
            Logger.debug(getClass(), ex.toString());
        }

        createIndex(dotConnect);
    }

    private void attemptDropIndex(final DotConnect dotConnect) throws SQLException{
        if (DbConnectionFactory.isPostgres()) {
            dotConnect.executeStatement(POSTGRES_DROP_INDEX);
        }
        if (DbConnectionFactory.isOracle()) {
            dotConnect.executeStatement(ORACLE_DROP_INDEX);
        }
        if (DbConnectionFactory.isMySql()) {
            dotConnect.executeStatement(MYSQL_DROP_INDEX);
        }
        if (DbConnectionFactory.isMsSql()) {
            dotConnect.executeStatement(MSSQL_DROP_INDEX);
        }
    }

    private void createIndex(final DotConnect dotConnect) throws SQLException{
        if (DbConnectionFactory.isPostgres()) {
            dotConnect.executeStatement(POSTGRES_CREATE_INDEX);
        }
        if (DbConnectionFactory.isOracle()) {
            dotConnect.executeStatement(ORACLE_CREATE_INDEX);
        }
        if (DbConnectionFactory.isMySql()) {
            dotConnect.executeStatement(MYSQL_CREATE_INDEX);
        }
        if (DbConnectionFactory.isMsSql()) {
            dotConnect.executeStatement(MSSQL_CREATE_INDEX);
        }
    }

}
