package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.common.db.ForeignKey;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * This task removes the next_step_id, constraint:
 *
 * <pre>
 *  next_step_id varchar(36) not null references workflow_step(id)
 * </pre>
 *
 *
 * @author Jose Castro
 * @version 4.3.0
 * @since Nov 1st, 2017
 */
public class Task04320WorkflowActionRemoveNextStepConstraint implements StartupTask {

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        final DotConnect dc = new DotConnect();
        if (DbConnectionFactory.isMsSql()) {
            try {
                dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
            } catch (SQLException e) {
                throw new DotRuntimeException(
                        "Transaction isolation level could not be set.", e);
            }
        }

        // SCHEMA CHANGES
        this.removeWorkflowActionStepIdWorkflowStepFK    ();
    } // executeUpgrade.

    private void removeWorkflowActionStepIdWorkflowStepFK() throws DotDataException {

        final DotDatabaseMetaData metaData = new DotDatabaseMetaData();
        final ForeignKey foreignKey        = metaData.findForeignKeys
                ("workflow_action", "workflow_step",
                        Arrays.asList("next_step_id"), Arrays.asList("id"));

        if (null != foreignKey) {

            try {
                Logger.info(this, "Droping the FK: " + foreignKey);
                metaData.dropForeignKey(foreignKey);
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }
        }
    }
}