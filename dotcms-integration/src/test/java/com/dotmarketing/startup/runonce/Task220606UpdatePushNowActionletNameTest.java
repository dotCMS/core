package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.workflows.actionlet.PushNowActionlet;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220606UpdatePushNowActionletNameTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * The constant NAME on the class PushNowActionlet got changed so this might affect the info on the  table workflow_action_class
     * Here we test that our upgrade task behaves when there's already an entry on  workflow_action_class but using the old Actionlet name and there inst one at all.
     * @throws Exception
     */
    @Test
    public void Test_Execute_Upgrade() throws Exception {
        String actionClassID = null;
        final WorkflowAPI api = APILocator.getWorkflowAPI();
        final Task220606UpdatePushNowActionletName task = new Task220606UpdatePushNowActionletName();
        try {
            final DotConnect dotConnect = new DotConnect();
            //So... if we don't have an entry for the PushNowActionlet lets mimic one
            final String name = dotConnect.setSQL("SELECT name FROM workflow_action_class WHERE clazz = ? ").addParam(PushNowActionlet.class.getName()).getString("name");
            if ( null == name) {

                Assert.assertFalse(task.forceRun());

                actionClassID = UUIDGenerator.generateUuid();
                //Use any valid action to avoid an integrity reference error
                final WorkflowAction anyAction = api.findAction(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser());
                final WorkflowActionClass workflowActionClass = new WorkflowActionClass();
                workflowActionClass.setName("LOL");
                workflowActionClass.setOrder(0);
                workflowActionClass.setClazz(PushNowActionlet.class.getName());
                workflowActionClass.setId(actionClassID);
                workflowActionClass.setActionId(anyAction.getId());
                api.saveActionClass(workflowActionClass, APILocator.systemUser());
            } else {
                //if there is one already lets change its name
                new DotConnect().setSQL(" UPDATE workflow_action_class SET name = ? WHERE clazz = ? ")
                        .addParam("LOL")
                        .addParam(PushNowActionlet.class.getName()).loadResult();
            }

            Assert.assertTrue(task.forceRun());
            task.executeUpgrade();
            Assert.assertFalse(task.forceRun());
        }finally {
            //Since we had to mimic a valid entry on workflow_action_class table
            //Now we need to clean up our mess, so we don't break any other test that might be created around this Actionlet in the future
            if(null != actionClassID){
                new DotConnect().setSQL(" DELETE FROM workflow_action_class WHERE id = ? ")
                        .addParam(actionClassID).loadObjectResults();
                CacheLocator.getWorkFlowCache().clearCache();
            }
        }
    }

}
