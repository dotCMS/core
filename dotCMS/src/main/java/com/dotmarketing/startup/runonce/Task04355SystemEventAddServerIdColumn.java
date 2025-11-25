

package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This upgrade task will add the column server_id to the system_event table.
 *
 * @author jsanca
 * @version 5.0
 *
 */
public class Task04355SystemEventAddServerIdColumn extends AbstractJDBCStartupTask {

    private static final Map<DbType, String> selectServerIdColumnSQLMap = Map.of(
            DbType.POSTGRESQL,   "SELECT server_id FROM system_event",
            DbType.MYSQL,        "SELECT server_id FROM system_event",
            DbType.ORACLE,       "SELECT server_id FROM system_event",
            DbType.MSSQL,        "SELECT server_id FROM system_event"
            );

    private static final Map<DbType, String> addServerIdColumnSQLMap = Map.of(
            DbType.POSTGRESQL,   "ALTER TABLE system_event ADD server_id varchar(36)",
            DbType.MYSQL,        "ALTER TABLE system_event ADD server_id varchar(36)",
            DbType.ORACLE,       "ALTER TABLE system_event ADD server_id varchar2(36)",
            DbType.MSSQL,        "ALTER TABLE system_event ADD server_id NVARCHAR(36)"
    );

    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().hasColumn("system_event", "server_id");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException {

        this.addServerIdColumn();
    }

    private boolean addServerIdColumn() throws DotDataException {

        boolean needToCreate = false;
        Logger.info(this, "Adding new 'server_id' column to 'system_event' table.");

        try {

            if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.setAutoCommit(true);
            }

            new DotConnect().setSQL(this.getServerIdColumnSQL()).loadObjectResults();
        } catch (Throwable e) {

            Logger.info(this, "Column 'system_event.server_id' does not exists, creating it");
            needToCreate = true;
            // in some databases if an error is throw the transaction is not longer valid
            this.closeAndStartTransaction();
        }

        if (needToCreate) {
            try {

                if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                    DbConnectionFactory.setAutoCommit(true);
                }

                new DotConnect().executeStatement(getAddServerIdColumnSQL());
            } catch (SQLException e) {
                throw new DotRuntimeException("The 'server_id' column could not be created.", e);
            } finally {
                this.commitAndCloseTransaction();
            }
        }

        return needToCreate;
    }


    private String getAddServerIdColumnSQL() {

        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

        return addServerIdColumnSQLMap.getOrDefault(dbType, null);
    }

    private String getServerIdColumnSQL() {

        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

        return selectServerIdColumnSQLMap.getOrDefault(dbType, null);
    }

    private void commitAndCloseTransaction() throws DotHibernateException {
        if (DbConnectionFactory.inTransaction()) {
            HibernateUtil.closeAndCommitTransaction();
        }
    }

    private void closeAndStartTransaction() throws DotHibernateException {

        HibernateUtil.closeSessionSilently();
        HibernateUtil.startTransaction();
    }

    @Override
    public String getPostgresScript() { return null; }

    @Override
    public String getMySQLScript() { return null; }

    @Override
    public String getOracleScript() { return null; }

    @Override
    public String getMSSQLScript() { return null; }

    @Override
    protected List<String> getTablesToDropConstraints() { return Collections.emptyList(); }


}
