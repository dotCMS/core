

package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

/**
 * This upgrade task remove the unique name constraint from the workflow_scheme table.
 *
 * @author jsanca
 * @version 5.0
 *
 */
public class Task04360WorkflowSchemeDropUniqueNameConstraint implements StartupTask {



    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    public void executeUpgrade() throws DotDataException {

        final DotDatabaseMetaData metaData = new DotDatabaseMetaData();
        if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
            DbConnectionFactory.setAutoCommit(true);
        }
        try {

            Logger.info(this, "Dropping the unique scheme name constraint from the workflow_scheme table");
            metaData.executeDropConstraint(DbConnectionFactory.getConnection(),
                    "workflow_scheme", "unique_workflow_scheme_name");
        } catch (SQLException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
    } // executeUpgrade.
}
