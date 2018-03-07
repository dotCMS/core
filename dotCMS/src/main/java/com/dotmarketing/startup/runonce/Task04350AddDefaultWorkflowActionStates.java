package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.Map;

/**
 * This upgrade task to add the  {@link com.dotmarketing.portlets.workflows.model.WorkflowState}: NEW, PUBLISHED, UNPUBLISHED, ARCHIVED
 *
 * @author jsanca
 * @version 5.0
 *
 */
public class Task04350AddDefaultWorkflowActionStates implements StartupTask {

    public static final String SYSTEM_WORKFLOW_ID            = WorkflowAPI.SYSTEM_WORKFLOW_ID;
    protected static String SELECT_WORKFLOW_ACTION_NO_SYSTEM =
            "select workflow_scheme.name as workflow_name, workflow_action.id as action_id, workflow_action.show_on as action_show_on from workflow_scheme, workflow_action where workflow_scheme.id = workflow_action.scheme_id and workflow_action.scheme_id != ?";
    protected static String UPDATE_WORKFLOW_STATE = "update workflow_action set show_on=? where id =?";
    protected static String WORKFLOW_DEFAULT_STATE           = WorkflowState.NEW + "," + WorkflowState.PUBLISHED + "," + WorkflowState.UNPUBLISHED + "," + WorkflowState.ARCHIVED;

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException {

        Logger.info(this, "Running the upgrade NEW, PUBLISHED, UNPUBLISHED, ARCHIVED");

        new DotConnect()
                        .setSQL(SELECT_WORKFLOW_ACTION_NO_SYSTEM)
                        .addParam(SYSTEM_WORKFLOW_ID)
                        .loadObjectResults()
                        .stream().forEach(this::updateRow);

    } // executeUpgrade.

    private void updateRow(final Map<String, Object> row) {

        final String workflowName         = (String) row.get("workflow_name");
        final String workflowActionId     = (String) row.get("action_id");
        final String workflowActionShowOn = UtilMethods.isSet(row.get("action_show_on")) && row.get("action_show_on").toString().trim().length() > 0?
                row.get("action_show_on") + "," + WORKFLOW_DEFAULT_STATE:
                WORKFLOW_DEFAULT_STATE;

        try {

            Logger.info(this, "Modifying the workflow: " + workflowActionId +
                            ", name: " + workflowName + "to add these states: " + workflowActionShowOn);

            new DotConnect().setSQL(UPDATE_WORKFLOW_STATE).addParam(workflowActionShowOn)
                    .addParam(workflowActionId).loadResult();
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage(), e);
        }
    }


}

