package com.dotcms.workflow.helper;

import com.dotcms.datagen.WorkflowDataGen;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test is for handling the merging between system action mappings for content types and schemes
 * @author jsanca
 */
public class TestSystemActionMappingsHandlerMerger extends BaseWorkflowIntegrationTest  {

    @Test
    public void test_merge_system_actions_for_schemes ()
            throws DotDataException, DotSecurityException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final WorkflowScheme scheme   = new WorkflowDataGen().name("TestWF"+System.currentTimeMillis())
                .nextPersistedWithDefaultStepsAndActions();

        Assert.assertNotNull(scheme);
        final List<WorkflowAction> actions = new ArrayList<>();
        final List<WorkflowStep>   steps   = workflowAPI.findSteps(scheme);

        Assert.assertNotNull(steps);
        Assert.assertEquals(2, steps.size());

        for (final WorkflowStep step : steps) {

            actions.addAll(workflowAPI.findActions(step, APILocator.systemUser()));
        }

        Assert.assertEquals(2, actions.size());
    }
}
