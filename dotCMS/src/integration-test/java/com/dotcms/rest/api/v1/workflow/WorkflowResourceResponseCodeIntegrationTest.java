package com.dotcms.rest.api.v1.workflow;

import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.addSteps;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.createScheme;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.createWorkflowActions;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.doCleanUp;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.liferay.portal.model.User;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkflowResourceResponseCodeIntegrationTest {

    private static WorkflowAPI workflowAPI;
    private static RoleAPI roleAPI;
    private static WorkflowResource workflowResource;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        workflowAPI =  APILocator.getWorkflowAPI();
        ContentletAPI contentletAPI = APILocator.getContentletAPI();
        roleAPI = APILocator.getRoleAPI();
        ContentHelper contentHelper = ContentHelper.getInstance();
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        WorkflowImportExportUtil workflowImportExportUtil = WorkflowImportExportUtil.getInstance();
        WorkflowHelper workflowHelper = new WorkflowHelper(workflowAPI, roleAPI, contentletAPI, permissionAPI,
                workflowImportExportUtil);
        ResponseUtil responseUtil = ResponseUtil.INSTANCE;

        final User admin = APILocator.systemUser();
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(admin);
        when(webResource
                .init(anyString(), anyBoolean(), any(HttpServletRequest.class), anyBoolean(),
                        anyString())).thenReturn(dataObject);

        workflowResource = new WorkflowResource(workflowHelper, contentHelper, workflowAPI,
                contentletAPI, responseUtil, permissionAPI, workflowImportExportUtil, webResource);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        doCleanUp(workflowResource, workflowAPI);
    }

    @Test
    public void Find_Scheme_Null_Content_Type(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findSchemes(request, null);
        assertEquals(Response.Status.OK.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Scheme_Invalid_Content_Type(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findSchemes(request, "LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_All_Schemes_Null_Content_Type(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAllSchemesAndSchemesByContentType(request, null);
        assertEquals(Status.OK.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_All_Schemes_Invalid_Content_Type(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAllSchemesAndSchemesByContentType(request, "LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Steps_Null_Content_Type(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findStepsByScheme(request,null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Steps_By_Scheme(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findStepsByScheme(request,"LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Steps_By_Scheme_Null(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findStepsByScheme(request,null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_By_Step() throws Exception{
        final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,1);
        final List<WorkflowAction> actions = createWorkflowActions(workflowResource, savedScheme, adminRole.getId(), workflowSteps);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findActionByStep(request,workflowSteps.get(0).getId(),actions.get(0).getId());
        assertEquals(Status.OK.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_By_Step_NonExisting_Step() throws Exception{
        final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,1);
        final List<WorkflowAction> actions = createWorkflowActions(workflowResource, savedScheme, adminRole.getId(), workflowSteps);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findActionByStep(request,"LOL",actions.get(0).getId());
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_By_Step_NonExisting_Action() throws Exception{
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,1);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findActionByStep(request,workflowSteps.get(0).getId(), "LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_By_Step_Invalid_Input_Expect_BadRequest() throws Exception{
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findActionByStep(request,null, null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions() throws Exception{
        final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,1);
        final List<WorkflowAction> actions = createWorkflowActions(workflowResource, savedScheme, adminRole.getId(), workflowSteps);
        final Response findResponse = workflowResource.findAction(request,actions.get(0).getId());
        assertEquals(Status.OK.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_Null_Input() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAction(request,null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Actions_NonExisting(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAction(request,"LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Available_Actions(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAvailableActions(request,"LOL");
        assertEquals(Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Available_Actions_Invalid_Input(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAvailableActions(request,null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Available_Default_Actions_Invalid_Input(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findAvailableDefaultActionsBySchemes(request,null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
    }
}
