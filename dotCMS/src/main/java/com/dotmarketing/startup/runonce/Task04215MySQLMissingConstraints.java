package com.dotmarketing.startup.runonce;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This upgrade task will create some constraints (if they no exists) on the table workflow_task
 * only for DB mysql, these constraints already exist on the sql file for the postgres and oracle DB
 * and for mssql there is another task that takes care of it.
 *
 * @author erickgonzalez
 * @version 4.2.0
 * @since Aug 24, 2017
 */
public class Task04215MySQLMissingConstraints extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return null;
    }

    @Override
    public String getMSSQLScript() {
        return null;
    }

    @Override
    public String getOracleScript() {
        return null;
    }

    @Override
    public String getMySQLScript() {
        String sql = "";
        final String fk_workflow_assign = "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_assign FOREIGN KEY (assigned_to) REFERENCES cms_role (id);";
        final String fk_workflow_task_asset = "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_task_asset FOREIGN KEY (webasset) REFERENCES identifier (id);";
        final String fk_workflow_step = "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_step FOREIGN KEY (status) REFERENCES workflow_step (id);";
        Connection conn = null;
        final List<String> tables = new ArrayList<String>(
                Arrays.asList("workflow_task"));
        try {
            conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            final List<ForeignKey> listForeignKeys = this.getForeingKeys(conn, tables, false);
            final List<String> listForeignKeysNames = new ArrayList<String>();
            for(ForeignKey foreignKey : listForeignKeys){
                listForeignKeysNames.add(foreignKey.fkName());
            }
            if(!listForeignKeysNames.contains("FK_workflow_assign")){
                sql = sql + fk_workflow_assign;
            }
            if(!listForeignKeysNames.contains("FK_workflow_task_asset")){
                sql = sql + fk_workflow_task_asset;
            }
            if(!listForeignKeysNames.contains("FK_workflow_step")){
                sql = sql + fk_workflow_step;
            }
        } catch (Exception e) {
            Logger.error(this,
                    String.format("Error running the upgrade task 4215",
                            e.getMessage()), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.error(this,
                        String.format("Error closing the connection",
                                ex.getMessage()), ex);
            }
        }

        return sql;
    }

    @Override
    public String getH2Script() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
