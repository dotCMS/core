

package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * This upgrade task will remove an unnecessary Foreign Key
 * on workflow_task table. Then, it makes a double check of
 * any potential orphan data, and it will proceed with its
 * deletion from DB.
 *
 * @author Jose Orsini
 * @version 4.3.0
 * @since Jan 11, 2018
 *
 */
public class Task04235RemoveFKFromWorkflowTaskTable extends AbstractJDBCStartupTask {

    public static final String DROP_ORPHAN_WF_TASKS = "DELETE FROM workflow_task WHERE NOT EXISTS (SELECT * FROM identifier)";
    public static final String DROP_CONSTRAINT_QUERY = "ALTER TABLE workflow_task DROP CONSTRAINT FK_workflow_task_asset";
    public static final String MYSQL_DROP_CONSTRAINT_QUERY = "ALTER TABLE workflow_task DROP FOREIGN KEY FK_workflow_task_asset";

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException {
        DotConnect dc = new DotConnect();

        try{

            //Check if FK: fk_workflow_task_asset exists
            boolean foundFK = false;
            final List<ForeignKey> listForeignKeys = this.getForeingKeys(DbConnectionFactory.getConnection(),
                Arrays.asList("workflow_task"), false);
            for (ForeignKey key : listForeignKeys) {
                if ("fk_workflow_task_asset".equalsIgnoreCase(key.fkName())) {
                    foundFK = true;
                    break;
                }
            }

            //Drop FK: fk_workflow_task_asset
            if (foundFK) {
                DbConnectionFactory.getConnection().setAutoCommit(true);
                if (DbConnectionFactory.isMySql()) {
                    dc.setSQL(MYSQL_DROP_CONSTRAINT_QUERY);
                } else {
                    dc.setSQL(DROP_CONSTRAINT_QUERY);
                }
                Logger.info(this, "Executing drop constraint query: " + dc.getSQL());
                dc.loadResult();
                DbConnectionFactory.getConnection().setAutoCommit(false);
            }

        } catch (SQLException e) {
            Logger.error(this,"Error dropping constraint: " + e.getMessage());
            throw new DotDataException(e);
        }

        //Drop orphan Workflow Tasks
        try {
            dc = new DotConnect();
            DbConnectionFactory.getConnection().setAutoCommit(true);
            dc.setSQL(DROP_ORPHAN_WF_TASKS);
            Logger.info(this, "Executing DELETE Query: " + dc.getSQL());
            dc.loadResult();
            DbConnectionFactory.getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            Logger.error(this,"Error running DELETE QUERY: " + e.getMessage());
            throw new DotDataException(e);
        }
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
