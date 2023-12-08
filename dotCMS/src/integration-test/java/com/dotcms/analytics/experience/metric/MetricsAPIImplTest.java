package com.dotcms.analytics.experience.metric;

import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.AnalyticsAPIImpl;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.WorkflowActionClassDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.actionlet.ArchiveContentActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static com.dotmarketing.portlets.workflows.model.WorkflowState.*;
import static com.dotmarketing.portlets.workflows.model.WorkflowState.UNPUBLISHED;
import static org.junit.Assert.assertEquals;

public class MetricsAPIImplTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link MetricsAPIImpl#getHotResults(User)}
     * When: the method is called
     * Should: returns the right values
     */
    @Test
    public void results() throws DotDataException, DotSecurityException, IOException {

        final String workflow_step_1 = "countAllWorkflowUniqueSubActions_step_1_" + System.currentTimeMillis();
        final String workflow_step_2 = "countAllWorkflowUniqueSubActions_step_2_" + System.currentTimeMillis();

        final String workflow_action_1 = "countAllWorkflowUniqueSubActions_action_1_" + System.currentTimeMillis();
        final String workflow_action_2 = "countAllWorkflowUniqueSubActions_action_2_" + System.currentTimeMillis();
        final String workflow_action_3 = "countAllWorkflowUniqueSubActions_action_3_" + System.currentTimeMillis();
        final String workflow_action_4 = "countAllWorkflowUniqueSubActions_action_4_" + System.currentTimeMillis();

        final List<Tuple2<String, List<Tuple3<String, String, Set<WorkflowState>>>>> workflowStepsAndActions_1 = Arrays
                .asList(
                        Tuple.of(workflow_step_1,
                                Arrays.asList(
                                        // First component of the Tuple is the desired Action-Name.
                                        // The Second Component is The Next-Step we desire to be pointed to by the current action.
                                        // Third is the show-When definition.
                                        Tuple.of(workflow_action_1, "Current Step", EnumSet.of(EDITING, UNLOCKED, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_2, workflow_step_2,  EnumSet.of(EDITING, UNLOCKED, NEW, UNPUBLISHED))
                                )
                        ),
                        Tuple.of(workflow_step_2,
                                Arrays.asList(
                                        Tuple.of(workflow_action_3, "Current Step", EnumSet.of(EDITING, LOCKED, NEW, PUBLISHED, UNPUBLISHED)),
                                        Tuple.of(workflow_action_4, workflow_step_1, EnumSet.of(LISTING, UNLOCKED, NEW, PUBLISHED, UNPUBLISHED))
                                )
                        )
                );

        final WorkflowScheme workflow_1 = new WorkflowDataGen()
                .stepAndAction(workflowStepsAndActions_1).nextPersistedWithStepsAndActions();

        final List<WorkflowAction> actions_1 = APILocator.getWorkflowAPI().findActions(workflow_1, APILocator.systemUser());

        new WorkflowActionClassDataGen(actions_1.get(0).getId()).actionClass(ArchiveContentActionlet.class).nextPersisted();

        final MetricResult hotResults = APILocator.getMetricsAPI().getHotResults(APILocator.systemUser());

        final Map<String, Object> mapResults = JsonUtil.getJsonFromString(hotResults.toString());

        assertEquals(1, mapResults.size());

        Map<String, Map<String, Object>> differentiatingFeatures = (Map<String, Map<String, Object>>) mapResults.get("KEY_FEATURES");
        assertEquals(1, differentiatingFeatures.size());

        Map<String, Object> workflowResults = differentiatingFeatures.get("WORKFLOW");
        assertEquals(6, workflowResults.size());

        assertEquals(APILocator.getContentTypeAPI(APILocator.systemUser()).countContentTypeAssignedToNotSystemWorkflow(),
                Long.parseLong(workflowResults.get("CONTENT_TYPES_NOT_USING_SYSTEM_WORKFLOW").toString()));
        assertEquals(APILocator.getWorkflowAPI().countAllSchemasUniqueSubActions(APILocator.systemUser()),
                Long.parseLong(workflowResults.get("WORKFLOW_UNIQUE_SUB_ACTIONS_COUNT").toString()));
        assertEquals(APILocator.getWorkflowAPI().countAllSchemasSteps(APILocator.systemUser()),
                Long.parseLong(workflowResults.get("WORKFLOW_STEPS_COUNT").toString()));
        assertEquals(APILocator.getWorkflowAPI().countAllSchemasActions(APILocator.systemUser()),
                Long.parseLong(workflowResults.get("WORKFLOW_ACTIONS_COUNT").toString()));
        assertEquals(APILocator.getWorkflowAPI().countWorkflowSchemes(APILocator.systemUser()),
                Integer.parseInt(workflowResults.get("WORKFLOW_SCHEMES_COUNT").toString()));
        assertEquals(APILocator.getWorkflowAPI().countAllSchemasSubActions(APILocator.systemUser()),
                Long.parseLong(workflowResults.get("WORKFLOW_SUB_ACTIONS_COUNT").toString()));
    }

    /**
     * Method to test: {@link MetricsAPIImpl#getHotResults(User)}
     * When: Not Admin User try to call the method
     * Should: throw a {@link DotSecurityException}
     */
    @Test(expected = DotSecurityException.class)
    public void resultsLimitUser() throws DotSecurityException {
        final User user = new UserDataGen().nextPersisted();
        APILocator.getMetricsAPI().getHotResults(user);
    }
}
