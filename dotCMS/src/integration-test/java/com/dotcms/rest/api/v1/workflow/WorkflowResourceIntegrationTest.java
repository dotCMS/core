package com.dotcms.rest.api.v1.workflow;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.form.WorkflowStepAddForm;
import com.dotcms.workflow.form.WorkflowStepUpdateForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.liferay.portal.model.User;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkflowResourceIntegrationTest {

    private static WorkflowAPI workflowAPI;
    private static ContentletAPI contentletAPI;
    private static RoleAPI roleAPI;
    private static ContentHelper contentHelper;
    private static ResponseUtil responseUtil;
    private static WorkflowHelper workflowHelper;
    private static PermissionAPI permissionAPI;
    private static WorkflowImportExportUtil workflowImportExportUtil;
    private static WorkflowResource workflowResource;

    private static final String ADMIN_DEFAULT_ID = "dotcms.org.1";
    private static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";
    private static final String ADMIN_NAME = "User Admin";

    //Markers to identify all the garbage created by this Test.  So we can clean-up at the end.
    private static final String SCHEME_NAME_PREFIX = "scheme::";
    private static final String STEP_NAME_PREFIX = "step::";
    private static final String ACTION_NAME_PREFIX = "action::";

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentletAPI = APILocator.getContentletAPI();
        roleAPI = APILocator.getRoleAPI();
        contentHelper = ContentHelper.getInstance();
        workflowHelper = new WorkflowHelper(workflowAPI, roleAPI, contentletAPI);
        responseUtil = ResponseUtil.INSTANCE;
        permissionAPI = APILocator.getPermissionAPI();
        workflowImportExportUtil = WorkflowImportExportUtil.getInstance();
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(ADMIN_DEFAULT_ID);
        when(user.getEmailAddress()).thenReturn(ADMIN_DEFAULT_MAIL);
        when(user.getFullName()).thenReturn(ADMIN_NAME);

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource.init(anyString(), anyBoolean(), any(HttpServletRequest.class), anyBoolean(), anyString())).thenReturn(dataObject);

        workflowResource = new WorkflowResource(workflowHelper, contentHelper, workflowAPI, contentletAPI, responseUtil, permissionAPI, workflowImportExportUtil, webResource);
    }

    /**
     * Creates dummy schemes
     * @return
     */
    private WorkflowScheme createScheme(){
        final String randomSchemaName =  SCHEME_NAME_PREFIX + RandomStringUtils.random(20, true, true);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowSchemeForm form = new WorkflowSchemeForm.Builder().schemeName(randomSchemaName).schemeDescription("").schemeArchived(false).build();
        final Response saveResponse = workflowResource.save(request,form);
        assertEquals(Response.Status.OK.getStatusCode(), saveResponse.getStatus());
        final ResponseEntityView savedEv = ResponseEntityView.class.cast(saveResponse.getEntity());
        return WorkflowScheme.class.cast(savedEv.getEntity());
    }

    /**
     * finds ALL schemes in the database
     * @return
     */
    @SuppressWarnings("unchecked")
    private  List<WorkflowScheme> findSchemes(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findSchemes(request, null);
        assertEquals(Response.Status.OK.getStatusCode(), findResponse.getStatus());
        final ResponseEntityView listEv = ResponseEntityView.class.cast(findResponse.getEntity());
        return List.class.cast(listEv.getEntity());
    }

    /**
     * Adds-up a number of steps to the given scheme
     * @param savedScheme
     * @param num
     * @return
     */
    private List<WorkflowStep> addSteps(final WorkflowScheme savedScheme, int num) {
        final List<WorkflowStep> workflowSteps = new ArrayList<>(2);
        for (int i = 0; i < num; i++) {
            final String randomStepName = STEP_NAME_PREFIX + RandomStringUtils.random(20, true, true);
            final HttpServletRequest addStepRequest = mock(HttpServletRequest.class);
            final WorkflowStepAddForm workflowStepAddForm = new WorkflowStepAddForm.Builder().stepName(randomStepName).schemeId(savedScheme.getId()).enableEscalation(false).escalationTime("0").escalationAction("").stepResolved(false).build();
            final Response addStepResponse = workflowResource.addStep(addStepRequest, workflowStepAddForm);
            assertEquals(Response.Status.OK.getStatusCode(), addStepResponse.getStatus());
            final ResponseEntityView savedStepEntityView = ResponseEntityView.class.cast(addStepResponse.getEntity());
            final WorkflowStep workflowStep = WorkflowStep.class.cast(savedStepEntityView.getEntity());
            assertNotNull(workflowStep);
            workflowSteps.add(workflowStep);
        }
        return workflowSteps;
    }

    /**
     * Performs a reorder command over the workflow resource
     * @param stepId
     * @param newPosition
     */
    private void reorderSteps(String stepId, int newPosition){
        final HttpServletRequest reorderStepRequest = mock(HttpServletRequest.class);
        final Response reorderStepResponse = workflowResource.reorderStep(reorderStepRequest, stepId, newPosition);
        assertEquals(Response.Status.OK.getStatusCode(), reorderStepResponse.getStatus());
        final ResponseEntityView reorderedStepEntityView = ResponseEntityView.class.cast(reorderStepResponse.getEntity());
        final String ok = String.class.cast(reorderedStepEntityView.getEntity());
        assertEquals("Ok",ok);
    }

    private List<WorkflowAction> createWorkflowActions(final WorkflowScheme savedScheme, final String roleId, List<WorkflowStep> workflowSteps){
        final List<WorkflowAction> workflowActions = new ArrayList<>(2);
        for(final WorkflowStep ws:workflowSteps) {
            final String randomActionName = ACTION_NAME_PREFIX + RandomStringUtils.random(20, true, true);
            final HttpServletRequest saveActionRequest = mock(HttpServletRequest.class);
            final Set<WorkflowState> states = WorkflowState.toSet(WorkflowState.values());
            final WorkflowActionForm form = new WorkflowActionForm.Builder().schemeId(savedScheme.getId()).
                    stepId(ws.getId()).
                    actionName(randomActionName).
                    showOn(states).
                    actionNextStep("currentStep").
                    actionAssignable(false).
                    actionCommentable(false).
                    requiresCheckout(false).
                    actionNextAssign(roleId).
                    whoCanUse(Arrays.asList("")).
                    actionCondition("").
                    build();
            final Response saveActionResponse = workflowResource.save(saveActionRequest, form);
            assertEquals(Response.Status.OK.getStatusCode(), saveActionResponse.getStatus());
            final ResponseEntityView savedActionEv = ResponseEntityView.class.cast(saveActionResponse.getEntity());
            final WorkflowAction savedAction = WorkflowAction.class.cast(savedActionEv.getEntity());
            assertNotNull(savedAction);
            workflowActions.add(savedAction);
        }
        return workflowActions;
    }

    @SuppressWarnings("unchecked")
    private List<WorkflowStep> findSteps(final WorkflowScheme savedScheme){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findStepsByScheme(request, savedScheme.getId());
        assertEquals(Response.Status.OK.getStatusCode(), findResponse.getStatus());
        final ResponseEntityView findResponseEv = ResponseEntityView.class.cast(findResponse.getEntity());
        return List.class.cast(findResponseEv.getEntity());
    }

    private void validateOrderedSteps(final List<WorkflowStep> steps){
        int lastOrderValue = -1;
        for(WorkflowStep step:steps){
            assertTrue(lastOrderValue < step.getMyOrder());
            lastOrderValue = step.getMyOrder();
        }
    }

    @Test
    public void testAddSchemaThenFindIt(){
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final List<WorkflowScheme> schemaList = findSchemes();
        boolean found = false;
        for(WorkflowScheme scheme:schemaList){
            found = savedScheme.getId().equals(scheme.getId());
            if(found){
               break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testUpdateSchema(){
        final String updatedName = "Any!";
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        WorkflowSchemeForm form = new WorkflowSchemeForm.Builder().schemeDescription("lol").schemeArchived(false).schemeName(updatedName).build();
        final Response updateResponse = workflowResource.update(request,savedScheme.getId(), form);
        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());
        final ResponseEntityView updateSchemeEntityView = ResponseEntityView.class.cast(updateResponse.getEntity());
        final WorkflowScheme updatedScheme = WorkflowScheme.class.cast(updateSchemeEntityView.getEntity());
        assertNotNull(updatedScheme);
    }

    @Test
    public void testUpdateSchemaUsingDupeName(){
        final WorkflowScheme savedScheme1 = createScheme();
        final WorkflowScheme savedScheme2 = createScheme();
        assertNotNull(savedScheme1);
        final String updatedName = savedScheme2.getName();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        WorkflowSchemeForm form = new WorkflowSchemeForm.Builder().schemeDescription("lol").schemeArchived(false).schemeName(updatedName).build();
        final Response updateResponse = workflowResource.update(request,savedScheme1.getId(), form);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void testCreateSchemeThenAddStepsThenAddActions() throws Exception{
        final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        final String adminRoleId = adminRole.getId();
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(savedScheme,3);
        assertFalse(workflowSteps.isEmpty());
        final List<WorkflowAction> actions = createWorkflowActions(savedScheme, adminRoleId, workflowSteps);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void testCreateSchemeThenAddStepsThenDeleteSteps() {
        final int numSteps = 5;
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(savedScheme, numSteps);
        assertFalse(workflowSteps.isEmpty());
        final HttpServletRequest removeStepRequest = mock(HttpServletRequest.class);
        int count = 0;
        for(final WorkflowStep ws: workflowSteps){
            Response deleteStepResponse = workflowResource.deleteStep(removeStepRequest, ws.getId());
            assertEquals(Response.Status.OK.getStatusCode(), deleteStepResponse.getStatus());
            count++;
        }
        assertEquals(count,numSteps);
    }

    @Test
    public void testCreateSchemeThenFindStepsBySchemeId() {
        int numSteps = 4;
        final WorkflowScheme savedScheme = createScheme();
        final List<WorkflowStep> steps = findSteps(savedScheme);
        validateOrderedSteps(steps);
        assertTrue(steps.isEmpty());
        addSteps(savedScheme,numSteps);
        final List<WorkflowStep> afterAddingSteps = findSteps(savedScheme);
        assertEquals(numSteps, afterAddingSteps.size());
        validateOrderedSteps(afterAddingSteps);
    }

    @Test
    public void testReorderStep() {
        final int numSteps = 10;
        final Random random = new Random();
        final int randomEntry = random.nextInt(numSteps);
        final WorkflowScheme savedScheme = createScheme();
        final List<WorkflowStep> steps = findSteps(savedScheme);
        assertTrue(steps.isEmpty());
        final List<WorkflowStep> workflowStepsOriginalSetUp = addSteps(savedScheme, numSteps);
        final WorkflowStep firstWorkflowStep = workflowStepsOriginalSetUp.get(0);
        reorderSteps(firstWorkflowStep.getId(),randomEntry);

        final List<WorkflowStep> afterReorderingSteps = findSteps(savedScheme);
        assertEquals(numSteps, afterReorderingSteps.size());
        final WorkflowStep rearrangedPosition = afterReorderingSteps.get(randomEntry);

        assertEquals(rearrangedPosition.getMyOrder(),randomEntry);
        assertEquals(rearrangedPosition.getId(),firstWorkflowStep.getId());
        validateOrderedSteps(afterReorderingSteps);
    }

    @Test
    public void testUpdateStep(){
        final int numSteps = 3;
        final String updatedName = "LOL!";
        final int newOrder = numSteps + 10; //We're sending an index outside the boundaries. See if we can break it.
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(savedScheme, numSteps);
        assertFalse(workflowSteps.isEmpty());
        final WorkflowStep workflowStep = workflowSteps.get(0);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowStepUpdateForm updatedValuesForm = new WorkflowStepUpdateForm.Builder().enableEscalation(false).escalationAction("").escalationTime("0").enableEscalation(false).stepName(updatedName).stepOrder(newOrder).build();
        final Response updateStepResponse = workflowResource.updateStep(request, workflowStep.getId(), updatedValuesForm);
        assertEquals(Response.Status.OK.getStatusCode(), updateStepResponse.getStatus());
        final ResponseEntityView updateResponseEv = ResponseEntityView.class.cast(updateStepResponse.getEntity());
        final WorkflowStep updatedWorkflowStep = WorkflowStep.class.cast(updateResponseEv.getEntity());
        assertEquals(updatedName, updatedWorkflowStep.getName());
        assertEquals(numSteps -1 ,updatedWorkflowStep.getMyOrder());
    }


}
