

package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This upgrade task will adds the personalization column to the multitree and will remove the pk and adds a new one include the personalization as part of it.
 *
 * @author jsanca
 * @version 5.0
 *
 */
public class Task05160MultiTreeAddPersonalizationColumnAndChangingPK extends AbstractJDBCStartupTask {

    private static final Map<DbType, String> addPersonalizationColumnSQLMap = Map.of(
            DbType.POSTGRESQL,   "ALTER TABLE multi_tree ADD personalization varchar(255)   not null default 'dot:default'",
            DbType.MYSQL,        "ALTER TABLE multi_tree ADD personalization varchar(255)   not null default 'dot:default'",
            DbType.ORACLE,       "ALTER TABLE multi_tree ADD personalization varchar2(255)  default 'dot:default' not null",
            DbType.MSSQL,        "ALTER TABLE multi_tree ADD personalization NVARCHAR(255)  not null default 'dot:default'"
    );

    final static String ALTER_TABLE_ADD_NEW_PK_WITH_PERSONALIZATION_COLUMN =   "ALTER TABLE multi_tree ADD CONSTRAINT idx_multitree_index1 PRIMARY KEY (parent1, parent2, child, relation_type,personalization)";

    @Override
    @CloseDBIfOpened
    public boolean forceRun() {

        try {

            final Set<String> columns = new DotDatabaseMetaData().getColumnNames
                    (DbConnectionFactory.getConnection(), "multi_tree");
            final Set<String> lowerColumns = columns.stream().map(String::toLowerCase).collect(Collectors.toSet());
            return !lowerColumns.contains("personalization");
        } catch (SQLException e) {

            return Boolean.TRUE;
        }
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException {

        if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
            DbConnectionFactory.setAutoCommit(true);
        }

        try {

            this.removeMultitreePK();
            this.addPersonalizationColumn();
            this.addNewMultitreePK();
        } catch (SQLException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
    } // executeUpgrade.

    private void removeMultitreePK () {

        Logger.info(this, "Dropping the PK of the multitree table");

        final List<String>     tables      = CollectionsUtils.list("multi_tree");
        final Connection       connection  = DbConnectionFactory.getConnection();
        final List<PrimaryKey> primaryKeys = this.getPrimaryKey(connection, tables, true);
        for (final PrimaryKey primaryKey : primaryKeys) {

            Logger.info(this, "Removed the PK: " + primaryKey);
        }
    }

    private void addPersonalizationColumn () throws SQLException {

        Logger.info(this, "Adding new 'personalization' column to 'multi_tree' table.");

        try {

            new DotConnect().executeStatement(getAddPersonalizationColumnSQL());
        } catch (SQLException e) {
            Logger.error(this, "The 'personalization' column could not be created.", e);
            throw  e;
        }
    }

    private String getAddPersonalizationColumnSQL() {

        final AbstractJDBCStartupTask.DbType dbType =
                AbstractJDBCStartupTask.DbType.getDbType(DbConnectionFactory.getDBType());

        return addPersonalizationColumnSQLMap.getOrDefault(dbType, null);
    }

    private void addNewMultitreePK () throws SQLException  {

        Logger.info(this, "Adding new 'PK' to 'multi_tree' table, including personalization column.");

        try {

            new DotConnect().executeStatement(ALTER_TABLE_ADD_NEW_PK_WITH_PERSONALIZATION_COLUMN);
        } catch (SQLException e) {
            Logger.error(this, "'PK' to 'multi_tree' table, including personalization column, could not be created.", e);
            throw  e;
        }
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
