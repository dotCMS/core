package com.dotcms.workflow.helper;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.UUIDUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test is for handling the merging between system action mappings for content types and schemes
 * @author jsanca
 */
public class TestSystemActionMappingsHandlerMerger extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * This test first creates an workflow with 2 steps and 2 actions.
     * Them assign the first action to NEW and them the second one to EDIT, PUBLISH, UNPUBLISH system actions.
     *
     * Them send a mappings to merge the first action to EDIT, and deletes the rest of them
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
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

        workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme(SystemAction.NEW, actions.get(0), scheme);
        workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme(SystemAction.EDIT, actions.get(1), scheme);
        workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme(SystemAction.PUBLISH, actions.get(1), scheme);
        workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme(SystemAction.UNPUBLISH, actions.get(1), scheme);

        final List<SystemActionWorkflowActionMapping>  systemActionWorkflowActionMappings = workflowAPI.findSystemActionsByScheme(scheme, APILocator.systemUser());
        Assert.assertEquals(4, systemActionWorkflowActionMappings.size());

        final SystemActionWorkflowActionMapping mappingToMerger = new SystemActionWorkflowActionMapping(
                UUIDUtil.uuid(), SystemAction.EDIT, actions.get(0), scheme);

        new SystemActionMappingsHandlerMerger(workflowAPI).mergeSystemActions(scheme, Arrays.asList(mappingToMerger));

        final List<SystemActionWorkflowActionMapping>  systemActionWorkflowActionMappingsMerged = workflowAPI.findSystemActionsByScheme(scheme, APILocator.systemUser());
        Assert.assertEquals(1, systemActionWorkflowActionMappingsMerged.size());
        Assert.assertEquals(SystemAction.EDIT, systemActionWorkflowActionMappingsMerged.get(0).getSystemAction());
        Assert.assertEquals(actions.get(0),    systemActionWorkflowActionMappingsMerged.get(0).getWorkflowAction());
    }


    /**
     * This test first creates an workflow with 2 steps and 2 actions and a content type
     * Them assign the first action to NEW and them the second one to EDIT, PUBLISH, UNPUBLISH system actions.
     *
     * Them send a mappings to merge the first action to EDIT, and deletes the rest of them
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void test_merge_system_actions_for_content_types ()
            throws DotDataException, DotSecurityException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final WorkflowScheme scheme   = new WorkflowDataGen().name("TestWF"+System.currentTimeMillis())
                .nextPersistedWithDefaultStepsAndActions();
        final ContentType contentType = new ContentTypeDataGen().workflowId(scheme.getId()).velocityVarName("ContentType"+System.currentTimeMillis())
                .nextPersisted();

        Assert.assertNotNull(scheme);
        final List<WorkflowAction> actions = new ArrayList<>();
        final List<WorkflowStep>   steps   = workflowAPI.findSteps(scheme);

        Assert.assertNotNull(steps);
        Assert.assertEquals(2, steps.size());

        for (final WorkflowStep step : steps) {

            actions.addAll(workflowAPI.findActions(step, APILocator.systemUser()));
        }

        Assert.assertEquals(2, actions.size());

        Assert.assertNotNull(contentType);

        workflowAPI.mapSystemActionToWorkflowActionForContentType(SystemAction.NEW, actions.get(0), contentType);
        workflowAPI.mapSystemActionToWorkflowActionForContentType(SystemAction.EDIT, actions.get(1), contentType);
        workflowAPI.mapSystemActionToWorkflowActionForContentType(SystemAction.PUBLISH, actions.get(1), contentType);
        workflowAPI.mapSystemActionToWorkflowActionForContentType(SystemAction.UNPUBLISH, actions.get(1), contentType);

        final List<SystemActionWorkflowActionMapping>  systemActionWorkflowActionMappings = workflowAPI.findSystemActionsByContentType(contentType, APILocator.systemUser());
        Assert.assertEquals(4, systemActionWorkflowActionMappings.size());

        final SystemActionWorkflowActionMapping mappingToMerger = new SystemActionWorkflowActionMapping(
                UUIDUtil.uuid(), SystemAction.EDIT, actions.get(0), contentType);

        new SystemActionMappingsHandlerMerger(workflowAPI).mergeSystemActions(contentType, Arrays.asList(mappingToMerger));

        final List<SystemActionWorkflowActionMapping>  systemActionWorkflowActionMappingsMerged = workflowAPI.findSystemActionsByContentType(contentType, APILocator.systemUser());
        Assert.assertEquals(1, systemActionWorkflowActionMappingsMerged.size());
        Assert.assertEquals(SystemAction.EDIT, systemActionWorkflowActionMappingsMerged.get(0).getSystemAction());
        Assert.assertEquals(actions.get(0),    systemActionWorkflowActionMappingsMerged.get(0).getWorkflowAction());
    }
}
