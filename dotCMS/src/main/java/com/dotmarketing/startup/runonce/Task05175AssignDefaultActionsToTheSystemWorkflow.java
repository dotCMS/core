package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;

/**
 * This upgrade task set to the system workflow the default actions
 * @author jsanca
 */
public class Task05175AssignDefaultActionsToTheSystemWorkflow implements StartupTask {

    @Override
    public boolean forceRun() {

        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException {

        this.checkInsertSystemAction(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID,      WorkflowAPI.SystemAction.NEW, WorkflowAPI.SystemAction.EDIT);
        this.checkInsertSystemAction(SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID,   WorkflowAPI.SystemAction.PUBLISH);
        this.checkInsertSystemAction(SystemWorkflowConstants.WORKFLOW_UNPUBLISH_ACTION_ID, WorkflowAPI.SystemAction.UNPUBLISH);
        this.checkInsertSystemAction(SystemWorkflowConstants.WORKFLOW_ARCHIVE_ACTION_ID,   WorkflowAPI.SystemAction.ARCHIVE);
        this.checkInsertSystemAction(SystemWorkflowConstants.WORKFLOW_UNARCHIVE_ACTION_ID, WorkflowAPI.SystemAction.UNARCHIVE);
        this.checkInsertSystemAction(SystemWorkflowConstants.WORKFLOW_DELETE_ACTION_ID,    WorkflowAPI.SystemAction.DELETE);

    } // executeUpgrade.

    private void checkInsertSystemAction (final String workflowActionId, final WorkflowAPI.SystemAction... systemActions) throws DotDataException {

        if (UtilMethods.isSet(new DotConnect().setSQL("select * from workflow_action where id = ? and scheme_id = ?")
                .addParam(workflowActionId).addParam(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                .loadObjectResults())) {

            for (final WorkflowAPI.SystemAction systemAction : systemActions) {
                insert(systemAction, workflowActionId);
            }
        }
    }

    private void insert (final WorkflowAPI.SystemAction systemAction, final String workflowActionId) throws DotDataException {

        if (!UtilMethods.isSet(new DotConnect()
                .setSQL("select * from workflow_action_mappings where action = ? and workflow_action = ? and scheme_or_content_type = ?")
                .addParam(systemAction.name()).addParam(workflowActionId)
                .addParam(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                .loadObjectResults())) {

            new DotConnect()
                    .setSQL("insert into workflow_action_mappings(id, action, workflow_action, scheme_or_content_type) values (?,?,?,?)")
                    .addParam(UUIDUtil.uuid()).addParam(systemAction.name())
                    .addParam(workflowActionId).addParam(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                    .loadObjectResults();
        }

    }
}
