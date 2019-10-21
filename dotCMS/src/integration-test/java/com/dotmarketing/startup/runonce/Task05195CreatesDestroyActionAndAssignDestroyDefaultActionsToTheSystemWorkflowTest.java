package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.util.UtilMethods;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05195CreatesDestroyActionAndAssignDestroyDefaultActionsToTheSystemWorkflowTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void removeValues() throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        try {

            if (!UtilMethods.isSet(new DotConnect().setSQL("select * from permission where inode_id = ?")
                    .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                    .loadObjectResults())) {

                dotConnect.setSQL("delete permission where inode_id = ?")
                        .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                        .loadResult();
            }
        }catch (DotDataException e){
            //Nah.
        }

        try {

            if (!UtilMethods.isSet(new DotConnect().setSQL("select * from workflow_action_class where action_id = ?")
                    .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                    .loadObjectResults())) {

                dotConnect.setSQL("delete workflow_action_class where action_id = ?")
                        .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                        .loadResult();
            }
        }catch (DotDataException e){
            //Nah.
        }

        try {

            if (!UtilMethods.isSet(new DotConnect().setSQL("select * from workflow_action_step where action_id = ? and step_id=?")
                    .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                    .addParam(SystemWorkflowConstants.WORKFLOW_ARCHIVE_STEP_ID)
                    .loadObjectResults())) {

                dotConnect.setSQL("delete workflow_action_step where action_id = ? and step_id=?")
                        .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                        .addParam(SystemWorkflowConstants.WORKFLOW_ARCHIVE_STEP_ID)
                        .loadResult();
            }
        }catch (DotDataException e){
            //Nah.
        }

        //insert into workflow_action (id, scheme_id, name, condition_to_progress, next_step_id, next_assign, my_order, assignable, commentable, icon, use_role_hierarchy_assign, requires_checkout, show_on) values (?, ?, ?, ?, ?, ?, ?,?, ?, ?,?,?,?)
        try {

            if (!UtilMethods.isSet(new DotConnect().setSQL("select * from workflow_action where id = ?")
                    .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                    .loadObjectResults())) {

                dotConnect.setSQL("delete workflow_action where id = ?")
                        .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                        .loadResult();
            }
        }catch (DotDataException e){
            //Nah.
        }

    }

    @Test
    public void Test_Upgrade_Task() throws DotDataException {
        removeValues();
        final Task05195CreatesDestroyActionAndAssignDestroyDefaultActionsToTheSystemWorkflow task =  new Task05195CreatesDestroyActionAndAssignDestroyDefaultActionsToTheSystemWorkflow();
        assertTrue(task.forceRun());
        task.executeUpgrade();
    }

}
