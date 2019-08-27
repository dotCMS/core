package com.dotmarketing.startup.runonce;

import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Config;
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
        final String UPDATE_MISSING_WORKFLOW_ASSIGNMENTS = "UPDATE workflow_task SET assigned_to = "
                + " (SELECT id FROM cms_role WHERE role_name = '" + Role.CMS_ADMINISTRATOR_ROLE + "') "
                + " WHERE NOT EXISTS (SELECT 1 FROM cms_role rl WHERE rl.id = assigned_to); ";

        final String FKWORKFLOWASSIGN = "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_assign FOREIGN KEY (assigned_to) REFERENCES cms_role (id);";
        final String FKWORKFLOWTASKASSET = "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_task_asset FOREIGN KEY (webasset) REFERENCES identifier (id);";
        final String FKWORKFLOWSTEP = "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_step FOREIGN KEY (status) REFERENCES workflow_step (id);";
        Connection conn = null;
        final List<String> tables = new ArrayList<>(
                Arrays.asList("workflow_task"));
        try {
            sql += UPDATE_MISSING_WORKFLOW_ASSIGNMENTS;
            conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            final List<ForeignKey> listForeignKeys = this.getForeingKeys(conn, tables, false);
            final List<String> listForeignKeysNames = listForeignKeys.stream().map(ForeignKey::fkName).collect(
                    CollectionsUtils.toImmutableList());
            if(!listForeignKeysNames.contains("FK_workflow_assign")){
                sql += FKWORKFLOWASSIGN;
            }
            if(!listForeignKeysNames.contains("FK_workflow_task_asset")){
                sql += FKWORKFLOWTASKASSET;
            }
            if(!listForeignKeysNames.contains("FK_workflow_step")){
                sql += FKWORKFLOWSTEP;
            }
        } catch (Exception e) {
            Logger.error(this,"Error Running Upgrade Task 4215 " + e.getMessage(),e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.error(this,"Error Closing the Connection " + ex.getMessage(),ex);
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
