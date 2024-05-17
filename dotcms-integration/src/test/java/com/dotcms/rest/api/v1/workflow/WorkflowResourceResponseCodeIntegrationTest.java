package com.dotcms.rest.api.v1.workflow;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.mock.response.MockAsyncResponse;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.*;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.*;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkflowResourceResponseCodeIntegrationTest {

    private static WorkflowAPI workflowAPI;
    private static RoleAPI roleAPI;
    private static WorkflowResource workflowResource;

    private Role roleAdmin () {

        //Creating a test role
        Role adminRole = null;
        try {

            adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
            if (adminRole == null) {
                adminRole = new RoleDataGen().key(ADMINISTRATOR).nextPersisted();
            }
        } catch (DotDataException e) {
            e.printStackTrace();
        }

        return adminRole;
    }

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        workflowAPI =  APILocator.getWorkflowAPI();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        roleAPI = APILocator.getRoleAPI();
        final ContentHelper contentHelper = ContentHelper.getInstance();
        final SystemActionApiFireCommandFactory systemActionApiFireCommandFactory =
                SystemActionApiFireCommandFactory.getInstance();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final WorkflowImportExportUtil workflowImportExportUtil = WorkflowImportExportUtil.getInstance();
        final WorkflowHelper workflowHelper = new WorkflowHelper(workflowAPI, roleAPI, contentletAPI, permissionAPI,
                workflowImportExportUtil);
        final ResponseUtil responseUtil = ResponseUtil.INSTANCE;

        final User admin = APILocator.systemUser();
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(admin);
        when(webResource
                .init(nullable(String.class), any(HttpServletRequest.class), any(HttpServletResponse.class), anyBoolean(),
                        nullable(String.class))).thenReturn(dataObject);

        workflowResource = new WorkflowResource(workflowHelper, contentHelper, workflowAPI,
                contentletAPI, responseUtil, permissionAPI, workflowImportExportUtil,new MultiPartUtils(), webResource,
                systemActionApiFireCommandFactory);
    }

    //@AfterClass
    public static void cleanup() throws Exception {
        doCleanUp(workflowAPI);
    }

    @Test
    public void Find_Scheme_Null_Content_Type(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findSchemes(request, new EmptyHttpResponse(), null, true);
        assertEquals(Response.Status.OK.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Scheme_Invalid_Content_Type(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findSchemes(request, new EmptyHttpResponse(), "LOL", true);
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_All_Schemes_Null_Content_Type(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAllSchemesAndSchemesByContentType(request, new EmptyHttpResponse(), null);
        assertEquals(Status.OK.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_All_Schemes_Invalid_Content_Type(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAllSchemesAndSchemesByContentType(request, new EmptyHttpResponse(), "LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Steps_Null_Content_Type(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findStepsByScheme(request,  new EmptyHttpResponse(),null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Steps_By_Scheme(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findStepsByScheme(request,  new EmptyHttpResponse(),"LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Steps_By_Scheme_Null(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findStepsByScheme(request,  new EmptyHttpResponse(),null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Save_Workflow_Scheme_Invalid_Ids() {
        final Role adminRole = this.roleAdmin();
        final String adminRoleId = adminRole.getId();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Set<WorkflowState> states = WorkflowState.toSet(WorkflowState.values());
        final String actionNewName = actionName();

        final WorkflowActionForm form = new WorkflowActionForm.Builder().
                schemeId("00").
                stepId("00").
                actionId("00").
                actionName(actionNewName).
                showOn(states).
                actionNextStep(CURRENT_STEP).
                actionAssignable(false).
                actionCommentable(false).
                requiresCheckout(false).
                actionNextAssign(adminRoleId).
                whoCanUse(List.of(adminRoleId)).
                actionCondition("").
                build();

        final Response findResponse = workflowResource.saveAction(request,  new EmptyHttpResponse(), form);
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_By_Step() throws Exception{
        final Role adminRole = this.roleAdmin();
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,1);
        final List<WorkflowAction> actions = createWorkflowActions(workflowResource, savedScheme, adminRole.getId(), workflowSteps);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findActionByStep(request,  new EmptyHttpResponse(), workflowSteps.get(0).getId(), actions.get(0).getId());
        assertEquals(Status.OK.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_By_Step_NonExisting_Step() throws Exception{
        final Role adminRole = this.roleAdmin();
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,1);
        final List<WorkflowAction> actions = createWorkflowActions(workflowResource, savedScheme, adminRole.getId(), workflowSteps);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findActionByStep(request,  new EmptyHttpResponse(),"LOL",actions.get(0).getId());
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_By_Step_NonExisting_Action() throws Exception{
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,1);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findActionByStep(request, new EmptyHttpResponse(), workflowSteps.get(0).getId(), "LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_By_Step_NonExisting_Step_Nor_Action() throws Exception{
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findActionByStep(request,  new EmptyHttpResponse(),"LOL", "LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }


    @Test
    public void Find_Actions_By_Step_Invalid_Input_Expect_BadRequest() throws Exception{
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findActionByStep(request, new EmptyHttpResponse(),null, null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions() throws Exception{
        final Role adminRole = this.roleAdmin();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,1);
        final List<WorkflowAction> actions = createWorkflowActions(workflowResource, savedScheme, adminRole.getId(), workflowSteps);
        final Response findResponse = workflowResource.findAction(request,  new EmptyHttpResponse(), actions.get(0).getId());
        assertEquals(Status.OK.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_By_Scheme_Invalid_Scheme() throws Exception{
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findActionsByScheme(request, new EmptyHttpResponse(), "LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Test_Update_Action_Expect404() throws Exception{
        final Role adminRole = this.roleAdmin();
        final String adminRoleId = adminRole.getId();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Set<WorkflowState> states = WorkflowState.toSet(WorkflowState.values());
        final WorkflowActionForm form = new WorkflowActionForm.Builder().schemeId("Any").
                actionName("").
                showOn(states).
                actionNextStep(CURRENT_STEP).
                actionAssignable(false).
                actionCommentable(false).
                requiresCheckout(false).
                actionNextAssign(adminRoleId).
                whoCanUse(Arrays.asList("")).
                actionCondition("").
                build();
        final String actionId = "-1";
        final Response updateResponse = workflowResource.updateAction(request,  new EmptyHttpResponse(), actionId, form);
        assertEquals(Status.NOT_FOUND.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void Update_Step_Invalid_Step_Expect_404() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowStepUpdateForm form = new WorkflowStepUpdateForm.Builder()
                .enableEscalation(false).escalationAction("000").escalationTime("").stepName("LOL")
                .stepOrder(1).stepResolved(false).build();
        final Response updateStepResponse = workflowResource.updateStep(request,  new EmptyHttpResponse(),"00", form);
        assertEquals(Status.NOT_FOUND.getStatusCode(), updateStepResponse.getStatus());
    }

    @Test
    public void Add_Step_Invalid_Step_Expect_404() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowStepAddForm form = new WorkflowStepAddForm.Builder().
                schemeId("0000").
                enableEscalation(false).
                escalationAction("000").
                escalationTime("").
                stepName("LOL").
                stepResolved(false).build();
        final Response updateStepResponse = workflowResource.addStep(request,  new EmptyHttpResponse(),form);
        assertEquals(Status.NOT_FOUND.getStatusCode(), updateStepResponse.getStatus());
    }

    @Test
    public void Find_Actions_Null_Input() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAction(request,  new EmptyHttpResponse(),null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_NonExisting(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAction(request,  new EmptyHttpResponse(),"LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Available_Actions(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAvailableActions(request,  new EmptyHttpResponse(),"LOL", null);
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Available_Actions_Invalid_Input(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAvailableActions(request, new EmptyHttpResponse(),null, null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Available_Default_Actions_Invalid_Input(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAvailableDefaultActionsBySchemes(request,  new EmptyHttpResponse(),null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Save_Actions_to_Step_Invalid_Scheme(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response saveActionToStepResponse = workflowResource.saveActionToStep(
                request,  new EmptyHttpResponse(),"0",
                new WorkflowActionStepForm.Builder().actionId("0").build()
        );

        assertEquals(Status.NOT_FOUND.getStatusCode(), saveActionToStepResponse.getStatus());
    }

    @Test
    public void Save_Action_Invalid_Ids_Expect_404() throws Exception{
        final Role adminRole = this.roleAdmin();
        final Set<WorkflowState> states = WorkflowState.toSet(WorkflowState.values());
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowActionForm form = new WorkflowActionForm.Builder().schemeId("00").
                stepId("00").
                actionName(actionName()).
                showOn(states).
                actionNextStep(CURRENT_STEP).
                actionAssignable(false).
                actionCommentable(false).
                requiresCheckout(false).
                actionNextAssign(adminRole.getId()).
                whoCanUse(Arrays.asList("")).
                actionCondition("").
                build();
        final Response saveActionToStepResponse = workflowResource.saveAction(
                request,  new EmptyHttpResponse(), form
        );
        assertEquals(Status.NOT_FOUND.getStatusCode(), saveActionToStepResponse.getStatus());
    }



    @Test
    public void Update_Action_Invalid_Scheme() throws Exception{
        final Role adminRole = this.roleAdmin();
        final Set<WorkflowState> states = WorkflowState.toSet(WorkflowState.values());
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowActionForm form = new WorkflowActionForm.Builder().schemeId("00").
                stepId("00").
                actionName(actionName()).
                showOn(states).
                actionNextStep(CURRENT_STEP).
                actionAssignable(false).
                actionCommentable(false).
                requiresCheckout(false).
                actionNextAssign(adminRole.getId()).
                whoCanUse(Arrays.asList("")).
                actionCondition("").
                build();
        final Response saveActionToStepResponse = workflowResource.updateAction(
                request,  new EmptyHttpResponse(),"000", form
        );
        assertEquals(Status.NOT_FOUND.getStatusCode(), saveActionToStepResponse.getStatus());
    }

    @Test
    public void Save_Action_to_Step_Invalid_Id() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response saveActionToStepResponse = workflowResource.saveActionToStep(request, new EmptyHttpResponse(),"00",new WorkflowActionStepForm.Builder().actionId("000").build());
        assertEquals(Status.NOT_FOUND.getStatusCode(), saveActionToStepResponse.getStatus());
    }

    @Test
    public void Delete_Step_Invalid_Id()  {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final AsyncResponse asyncResponse = new MockAsyncResponse((arg) -> {

            final Response saveActionToStepResponse = (Response)arg;
            assertEquals(Status.NOT_FOUND.getStatusCode(), saveActionToStepResponse.getStatus());
            return true;
        }, arg -> {
            fail("Error on deleting step");
            return true;
        });

        workflowResource.deleteStep(request, asyncResponse,"00");
    }

    @Test
    public void Delete_Action_Null_Id()  {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response saveActionToStepResponse = workflowResource.deleteAction(request,  new EmptyHttpResponse(),null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), saveActionToStepResponse.getStatus());
    }

    @Test
    public void Reorder_Action_Invalid_Id()  {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        WorkflowReorderWorkflowActionStepForm form = new WorkflowReorderWorkflowActionStepForm.Builder().order(-1).build();
        final Response reorderActionResponse = workflowResource.reorderAction(request, new EmptyHttpResponse(),"00","00", form );
        assertEquals(Status.NOT_FOUND.getStatusCode(), reorderActionResponse.getStatus());
    }

    @Test
    public void Reorder_Step_Invalid_Id()  {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response reorderStepResponse = workflowResource.reorderStep(request, new EmptyHttpResponse(),"LOL",0 );
        assertEquals(Status.NOT_FOUND.getStatusCode(), reorderStepResponse.getStatus());
    }

    @Test
    public void Delete_Action_Invalid_Step_Id()  {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response reorderStepResponse = workflowResource.deleteAction(request, new EmptyHttpResponse(),"LOL","LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), reorderStepResponse.getStatus());
    }

    @Test
    public void Find_Actions_By_Scheme_Invalid_SchemeId()  {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findSchemeByActionResponse = workflowResource.findActionsByScheme(request, new EmptyHttpResponse(),"LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findSchemeByActionResponse.getStatus());
    }

}
