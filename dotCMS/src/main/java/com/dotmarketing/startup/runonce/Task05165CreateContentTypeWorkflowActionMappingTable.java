package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * This upgrade task will creates the workflow_action_mappings and unique index for it.
 *
 * @author jsanca
 * @version 5.2
 *
 */
public class Task05165CreateContentTypeWorkflowActionMappingTable extends AbstractJDBCStartupTask {

    private static final Map<DbType, String> createContentTypeWorkflowActionMappingTableSQLMap = Map.of(

            DbType.POSTGRESQL,   "create table workflow_action_mappings (\n" +
                    "\n" +
                    "   id varchar(36) not null,\n" +
                    "   action varchar(36) not null,\n" +
                    "   workflow_action varchar(255) not null,\n" +
                    "   scheme_or_content_type  varchar(255) not null,\n" +
                    "   primary key (id)\n" +
                    ")",
            DbType.MYSQL,        "create table workflow_action_mappings (\n" +
                    "\n" +
                    "   id varchar(36) not null,\n" +
                    "   action varchar(36) not null,\n" +
                    "   workflow_action varchar(255) not null,\n" +
                    "   scheme_or_content_type  varchar(255) not null,\n" +
                    "   primary key (id)\n" +
                    ")",
            DbType.ORACLE,       "create table workflow_action_mappings (\n" +
                    "\n" +
                    "   id varchar2(36) not null primary key ,\n" +
                    "   action varchar2(36) not null,\n" +
                    "   workflow_action varchar2(255) not null,\n" +
                    "   scheme_or_content_type  varchar2(255) not null\n" +
                    ")",
            DbType.MSSQL,        "create table workflow_action_mappings (\n" +
                    "\n" +
                    "   id NVARCHAR(36) primary key,\n" +
                    "   action NVARCHAR(36) not null,\n" +
                    "   workflow_action NVARCHAR(255) not null,\n" +
                    "   scheme_or_content_type  NVARCHAR(255) not null\n" +
                    ")"
    );

    private static final Map<DbType, String> createContentTypeWorkflowActionMappingTableUniqueIndexSQLMap = Map.of(
            DbType.POSTGRESQL,   "CREATE UNIQUE INDEX idx_workflow_action_mappings ON workflow_action_mappings (action, workflow_action, scheme_or_content_type)",
            DbType.MYSQL,        "CREATE UNIQUE INDEX idx_workflow_action_mappings ON workflow_action_mappings (action, workflow_action, scheme_or_content_type)",
            DbType.ORACLE,       "CREATE UNIQUE INDEX idx_workflow_action_mappings ON workflow_action_mappings (action, workflow_action, scheme_or_content_type)",
            DbType.MSSQL,        "CREATE UNIQUE INDEX idx_workflow_action_mappings ON workflow_action_mappings (action, workflow_action, scheme_or_content_type)"
    );

    @Override
    @CloseDBIfOpened
    public boolean forceRun() {

        try {

            return !new DotDatabaseMetaData().tableExists(
                    DbConnectionFactory.getConnection(), "workflow_action_mappings");
        } catch (SQLException e) {

            return Boolean.FALSE;
        }
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException {

        if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
            DbConnectionFactory.setAutoCommit(true);
        }

        try {

            this.createContentTypeWorkflowActionMappingTable();
            this.createContentTypeWorkflowActionMappingUniqueIndex();
        } catch (SQLException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
    } // executeUpgrade.

    private void createContentTypeWorkflowActionMappingTable() throws SQLException {

        Logger.info(this, "Creates the table workflow_action_mappings.");

        try {

            new DotConnect().executeStatement(getCreateContentTypeWorkflowActionMappingTableSQL());
        } catch (SQLException e) {
            Logger.error(this, "The table 'workflow_action_mappings' could not be created.", e);
            throw  e;
        }
    }

    private String getCreateContentTypeWorkflowActionMappingTableSQL() {

        final DbType dbType =
                DbType.getDbType(DbConnectionFactory.getDBType());

        return createContentTypeWorkflowActionMappingTableSQLMap.get(dbType);
    }

    private void createContentTypeWorkflowActionMappingUniqueIndex() throws SQLException {

        Logger.info(this, "Creates the table idx_workflow_action_mappings unique index.");

        try {

            new DotConnect().executeStatement(getCreateContentTypeWorkflowActionMappingUniqueIndexSQL());
        } catch (SQLException e) {
            Logger.error(this, "The index for the table 'idx_workflow_action_mappings' could not be created.", e);
            throw  e;
        }
    }

    private String getCreateContentTypeWorkflowActionMappingUniqueIndexSQL() {

        final DbType dbType =
                DbType.getDbType(DbConnectionFactory.getDBType());

        return createContentTypeWorkflowActionMappingTableUniqueIndexSQLMap.get(dbType);
    }

    @Override
    public String getPostgresScript() {
        return null;
    }

    @Override
    public String getMySQLScript() {
        return null;
    }

    @Override
    public String getOracleScript() {
        return null;
    }

    @Override
    public String getMSSQLScript() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }


}
