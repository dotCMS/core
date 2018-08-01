package com.dotmarketing.startup.runonce;

import com.dotcms.util.ConversionUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.util.List;
import java.util.Map;

/**
 * This task updates the show_on column in order to add the Editing or Listing state depending on the requires checkout legacy column:
 *
 * Show On Listing if the action does NOT have a Requires Lock
 * Show On Editing if the action HAVE a Requires Lock
 *
 * @author jsanca
 * @version 5.0
 */
public class Task04390ShowEditingListingWorkflowActionTable implements StartupTask {


    private static final String MYSQL_FIND_SHOW_ON_OPTION_COLUMN      = "SELECT id, requires_checkout, show_on FROM workflow_action WHERE scheme_id <> ?";
    private static final String POSTGRES_FIND_SHOW_ON_OPTION_COLUMN   = "SELECT id, requires_checkout, show_on FROM workflow_action WHERE scheme_id <> ?";
    private static final String MSSQL_FIND_SHOW_ON_OPTION_COLUMN      = "SELECT id, requires_checkout, show_on FROM workflow_action WHERE scheme_id <> ?";
    private static final String ORACLE_FIND_SHOW_ON_OPTION_COLUMN     = "SELECT id, requires_checkout, show_on FROM workflow_action WHERE scheme_id <> ?";

    private static final String MYSQL_UPDATE_SHOW_ON_OPTION_COLUMN     = "UPDATE workflow_action SET show_on = ?  WHERE id = ?";
    private static final String POSTGRES_UPDATE_SHOW_ON_OPTION_COLUMN  = "UPDATE workflow_action SET show_on = ?  WHERE id = ?";
    private static final String MSSQL_UPDATE_SHOW_ON_OPTION_COLUMN     = "UPDATE workflow_action SET show_on = ?  WHERE id = ?";
    private static final String ORACLE_UPDATE_SHOW_ON_OPTION_COLUMN    = "UPDATE workflow_action SET show_on = ?  WHERE id = ?";

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    public void executeUpgrade() throws DotDataException {

        this.updateWorkflowActionData();
    } // executeUpgrade.

    private void updateWorkflowActionData() throws DotDataException {

        final DotConnect dc = new DotConnect();
        Logger.info(this, "Adding the Editing or Listing state to the show on actions.");

        final List<Map<String, Object>> actions =
                dc.setSQL(this.selectActionsSQL()).addObject(WorkflowAPI.SYSTEM_WORKFLOW_ID).loadObjectResults();
        actions.stream().forEach(row -> {

            try {

                dc.setSQL(updateShowOnActions())
                        .addParam(this.isLocked(row.get("requires_checkout"))?
                                row.get("show_on") +","+ WorkflowState.EDITING.name():
                                row.get("show_on") +","+ WorkflowState.LISTING.name() +","+ WorkflowState.EDITING.name())
                        .addParam(row.get("id").toString())
                        .loadResult();
            } catch (DotDataException e) {
                throw new DotRuntimeException(
                        "An error occurred when adding the show on to the workflow actions.",
                        e);
            }
        });
    } // updateWorkflowActionData.

    private String updateShowOnActions() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_UPDATE_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_UPDATE_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_UPDATE_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_UPDATE_SHOW_ON_OPTION_COLUMN;
        }

        return sql;
    }

    private String selectActionsSQL() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_FIND_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_FIND_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_FIND_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_FIND_SHOW_ON_OPTION_COLUMN;
        }

        return sql;
    }


    private boolean isLocked(final Object requiresCheckout) {

        return (null != requiresCheckout)?
                ConversionUtils.toBooleanFromDb(requiresCheckout):false;
    }

}
