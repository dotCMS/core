package com.dotcms.rest.api.v1.workflow;

import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.addSteps;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.createContentTypeAndAssignPermissions;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.createImportExportObjectForm;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.createScheme;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.createWorkflowActions;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.doCleanUp;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.findSchemes;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.findSteps;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.schemeName;
import static com.dotcms.rest.api.v1.workflow.WorkflowResourceTestUtil.stepName;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.WorkflowActionStepForm;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.form.WorkflowSchemeImportObjectForm;
import com.dotcms.workflow.form.WorkflowStepAddForm;
import com.dotcms.workflow.form.WorkflowStepUpdateForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIImpl;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkflowResourceLicenseIntegrationTest {

    private static RoleAPI roleAPI;
    private static WorkflowAPI licensedWorkflowAPI;
    private static WorkflowHelper licensedWorkflowHelper;
    private static WorkflowResource licenseWorkflowResource;

    private static WorkflowAPI nonLicensedWorkflowAPI;
    private static WorkflowHelper nonLicensedWorkflowHelper;
    private static WorkflowResource nonLicenseWorkflowResource;

    private static final int editPermission = PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT;

    private static User userAdmin;
    private static User billIntranet;

    private static Role publisher;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        licensedWorkflowAPI = APILocator.getWorkflowAPI();
        nonLicensedWorkflowAPI = new WorkflowAPIImpl(
                () -> false);// We override the license validity supplier to always deny having it.
        ContentletAPI contentletAPI = APILocator.getContentletAPI();
        roleAPI = APILocator.getRoleAPI();
        ContentHelper contentHelper = ContentHelper.getInstance();
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        WorkflowImportExportUtil workflowImportExportUtil = WorkflowImportExportUtil.getInstance();

        licensedWorkflowHelper = new WorkflowHelper(licensedWorkflowAPI, roleAPI, contentletAPI,
                permissionAPI,
                workflowImportExportUtil);

        nonLicensedWorkflowHelper = new WorkflowHelper(nonLicensedWorkflowAPI, roleAPI,
                contentletAPI, permissionAPI,
                workflowImportExportUtil);
        ResponseUtil responseUtil = ResponseUtil.INSTANCE;

        userAdmin = APILocator.systemUser();
        billIntranet = APILocator.getUserAPI().loadUserById("dotcms.org.2806");

        publisher = roleAPI.findRoleByName("Publisher / Legal", null);

        final WebResource webResourceThatReturnsAdminUser = mock(WebResource.class);
        final InitDataObject dataObject1 = mock(InitDataObject.class);
        when(dataObject1.getUser()).thenReturn(userAdmin);
        when(webResourceThatReturnsAdminUser
                .init(anyString(), anyBoolean(), any(HttpServletRequest.class), anyBoolean(),
                        anyString())).thenReturn(dataObject1);


        final WebResource webResourceThatReturnsARandomUser = mock(WebResource.class);
        final InitDataObject dataObject2 = mock(InitDataObject.class);
        when(dataObject2.getUser()).thenReturn(billIntranet);
        when(webResourceThatReturnsARandomUser
                .init(anyString(), anyBoolean(), any(HttpServletRequest.class), anyBoolean(),
                        anyString())).thenReturn(dataObject2);

        licenseWorkflowResource = new WorkflowResource(licensedWorkflowHelper, contentHelper,
                licensedWorkflowAPI,
                contentletAPI, responseUtil, permissionAPI, workflowImportExportUtil,
                webResourceThatReturnsAdminUser);

        nonLicenseWorkflowResource = new WorkflowResource(nonLicensedWorkflowHelper, contentHelper,
                nonLicensedWorkflowAPI,
                contentletAPI, responseUtil, permissionAPI, workflowImportExportUtil,
                webResourceThatReturnsARandomUser); //Returns Bill
    }


    @AfterClass
    public static void cleanup() throws Exception {
        doCleanUp(licenseWorkflowResource, licensedWorkflowAPI);
    }

    @Test
    public void Create_Schema_Invalid_License() {
        final String randomSchemaName = schemeName();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowSchemeForm form = new WorkflowSchemeForm.Builder()
                .schemeName(randomSchemaName).schemeDescription("").schemeArchived(false).build();
        final Response saveResponse = nonLicenseWorkflowResource.save(request, form);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), saveResponse.getStatus());
    }

    @Test
    public void Delete_Scheme_Invalid_License(){
        final WorkflowScheme savedScheme = createScheme(licenseWorkflowResource);
        final HttpServletRequest request1 = mock(HttpServletRequest.class);
        final Response response = nonLicenseWorkflowResource.delete(request1,savedScheme.getId());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void Find_All_Schemas_Invalid_License_Then_Only_Get_System_Workflow() {
        //Should Only return the system workflow
        final List<WorkflowScheme> schemes = findSchemes(nonLicenseWorkflowResource);
        assertEquals(1, schemes.size());
        assertTrue(schemes.get(0).isSystem());
    }

    @Test
    public void Find_Actions_By_Schemas_InvalidLicense() throws Exception {

        final WorkflowScheme defaultScheme = licensedWorkflowAPI.findDefaultScheme();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = nonLicenseWorkflowResource
                .findActionsByScheme(request, defaultScheme.getId());
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Action_InvalidLicense() throws Exception {
        final WorkflowScheme defaultScheme = licensedWorkflowAPI.findDefaultScheme();
        assertNotNull("Unable to find default scheme", defaultScheme);
        final List<WorkflowAction> actions = licensedWorkflowAPI
                .findActions(defaultScheme, APILocator.systemUser());
        assertFalse("Default scheme has no actions ", actions.isEmpty());

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = nonLicenseWorkflowResource
                .findAction(request, actions.get(0).getId());
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Action_By_Step_Invalid_License() throws Exception {
        //we use
        final WorkflowScheme defaultScheme = licensedWorkflowAPI.findDefaultScheme();
        assertNotNull("Unable to find default scheme", defaultScheme);
        final List<WorkflowAction> actions = licensedWorkflowAPI
                .findActions(defaultScheme, APILocator.systemUser());
        assertFalse("Default scheme has no actions ", actions.isEmpty());
        final List<WorkflowStep> steps = licensedWorkflowAPI.findSteps(defaultScheme);

        final Optional<WorkflowAction> o = actions.stream().filter(workflowAction -> "Assign Workflow".equals(workflowAction.getName())).findFirst();
        assertTrue(o.isPresent());
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = nonLicenseWorkflowResource
                .findActionByStep(request, steps.get(0).getId(), o.get().getId());
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_Steps_By_Scheme_Invalid_License() throws Exception {
        final WorkflowScheme defaultScheme = licensedWorkflowAPI.findDefaultScheme();
        assertNotNull("Unable to find default scheme", defaultScheme);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = nonLicenseWorkflowResource
                .findStepsByScheme(request, defaultScheme.getId());
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), findResponse.getStatus());
    }

    @Test
    public void Find_All_Schemes_By_Content_Type_Invalid_License_Expect_Only_System_Workflow()
            throws Exception {
        final WorkflowScheme defaultScheme = licensedWorkflowAPI.findDefaultScheme();
        assertNotNull("Unable to find default scheme", defaultScheme);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = nonLicenseWorkflowResource
                .findAllSchemesAndSchemesByContentType(request, null);
        assertEquals(Status.OK.getStatusCode(), findResponse.getStatus());
        ResponseEntityView ev = ResponseEntityView.class.cast(findResponse.getEntity());
        final SchemesAndSchemesContentTypeView view = SchemesAndSchemesContentTypeView.class.cast(ev.getEntity());
        //Should only return the system workflow
        assertEquals(1, view.getContentTypeSchemes().size());
        final WorkflowScheme scheme1 = view.getContentTypeSchemes().get(0);
        assertTrue(scheme1.isSystem());
        assertEquals(1, view.getSchemes().size());
        final WorkflowScheme scheme2 = view.getSchemes().get(0);
        assertTrue(scheme2.isSystem());
    }

    @Test
    public void Find_All_Schemes_By_Content_Type_Regular_User_InvalidLicense()
            throws Exception {

        ContentType contentType = null;
        final String ctName = "KeepWfTaskStatus" + System.currentTimeMillis();
        try {
            //Bill does NOT have Read permissions over this new content type
            contentType = createContentTypeAndAssignPermissions(ctName,
                    BaseContentType.CONTENT, editPermission, publisher.getId());

            final HttpServletRequest request = mock(HttpServletRequest.class);
            final Response findResponse = nonLicenseWorkflowResource
                    .findAllSchemesAndSchemesByContentType(request, contentType.id());
            assertEquals(Status.UNAUTHORIZED.getStatusCode(), findResponse.getStatus());
        } finally {
            if(contentType != null){
               APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
            }
        }
    }

    @Test
    public void Update_Schema_Invalid_License(){
        final String updatedName = schemeName();
        final WorkflowScheme savedScheme = createScheme(licenseWorkflowResource);
        assertNotNull(savedScheme);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        WorkflowSchemeForm form = new WorkflowSchemeForm.Builder().schemeDescription("lol").schemeArchived(false).schemeName(updatedName).build();
        final Response updateResponse = nonLicenseWorkflowResource.update(request,savedScheme.getId(), form);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void Add_Steps_Invalid_License(){

        final WorkflowScheme savedScheme = createScheme(licenseWorkflowResource);

        final HttpServletRequest addStepRequest = mock(HttpServletRequest.class);
        final WorkflowStepAddForm workflowStepAddForm = new WorkflowStepAddForm.Builder()
                .stepName(stepName()).schemeId(savedScheme.getId()).enableEscalation(false)
                .escalationTime("0").escalationAction("").stepResolved(false).build();
        final Response addStepResponse = nonLicenseWorkflowResource
                .addStep(addStepRequest, workflowStepAddForm);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), addStepResponse.getStatus());
    }

    @Test
    public void Reorder_Step_Invalid_License() {
        final int numSteps = 10;
        final WorkflowScheme savedScheme = createScheme(licenseWorkflowResource);
        final List<WorkflowStep> steps = findSteps(licenseWorkflowResource, savedScheme);
        assertTrue(steps.isEmpty());
        final List<WorkflowStep> workflowStepsOriginalSetUp = addSteps(licenseWorkflowResource, savedScheme, numSteps);
        final WorkflowStep firstWorkflowStep = workflowStepsOriginalSetUp.get(0);

        final String stepId = firstWorkflowStep.getId();
        int newPosition = 3;
        final HttpServletRequest reorderStepRequest = mock(HttpServletRequest.class);
        final Response reorderStepResponse = nonLicenseWorkflowResource
                .reorderStep(reorderStepRequest, stepId, newPosition);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), reorderStepResponse.getStatus());

    }

    @Test
    public void Update_Step_Invalid_License(){
        final int numSteps = 3;
        final String updatedName = "LOL!";
        final int newOrder = numSteps + 10;
        final WorkflowScheme savedScheme = createScheme(licenseWorkflowResource);
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(licenseWorkflowResource, savedScheme, numSteps);
        assertFalse(workflowSteps.isEmpty());
        final WorkflowStep workflowStep = workflowSteps.get(0);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowStepUpdateForm updatedValuesForm = new WorkflowStepUpdateForm.Builder().enableEscalation(false).escalationAction("").escalationTime("0").enableEscalation(false).stepName(updatedName).stepOrder(newOrder).build();
        final Response updateStepResponse = nonLicenseWorkflowResource.updateStep(request, workflowStep.getId(), updatedValuesForm);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), updateStepResponse.getStatus());
    }

    @Test
    public void Save_Action_To_Step_Invalid_License() throws Exception{
        final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        final String adminRoleId = adminRole.getId();
        final WorkflowScheme savedScheme = createScheme(licenseWorkflowResource);
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(licenseWorkflowResource, savedScheme,2);
        assertFalse(workflowSteps.isEmpty());
        assertEquals(2, workflowSteps.size());
        final List<WorkflowAction> actions = createWorkflowActions(licenseWorkflowResource, savedScheme, adminRoleId, workflowSteps);
        assertEquals(2, actions.size());

        final WorkflowStep secondStep = workflowSteps.get(1);
        final WorkflowAction firstAction = actions.get(0);

        final HttpServletRequest request1 = mock(HttpServletRequest.class);
        //assign the first action to the second step
        final Response saveActionToStepResponse = nonLicenseWorkflowResource.saveActionToStep(
                request1, secondStep.getId(),
                new WorkflowActionStepForm.Builder().actionId(firstAction.getId()).build()
        );
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), saveActionToStepResponse.getStatus());
    }

    @Test
    public void Import_Invalid_License() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowSchemeImportObjectForm form = createImportExportObjectForm();
        final Response response = nonLicenseWorkflowResource.importScheme(request,form);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void Export_Invalid_License() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowScheme savedScheme = createScheme(licenseWorkflowResource);
        final Response exportResponse = nonLicenseWorkflowResource.exportScheme(request, savedScheme.getId());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), exportResponse.getStatus());
    }


    @SuppressWarnings("unchecked")
    @Test
    public void Find_Available_Actions_Invalid_License() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        List<Contentlet> contetlets = APILocator.getContentletAPI().findAllContent(0,1);
        final Response response = nonLicenseWorkflowResource.findAvailableActions(request, contetlets.get(0).getInode());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        ResponseEntityView ev = ResponseEntityView.class.cast(response.getEntity());
        List<WorkflowAction> actions = List.class.cast(ev.getEntity());
        for(WorkflowAction action:actions){
            assertEquals(WorkflowScheme.SYSTEM_WORKFLOW_ID,action.getSchemeId());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void Find_Available_Default_Actions_Invalid_License() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final ContentType defaultContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).findDefault();
        final Response response = nonLicenseWorkflowResource.findAvailableDefaultActionsByContentType(request, defaultContentType.id());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        ResponseEntityView ev = ResponseEntityView.class.cast(response.getEntity());
        List<WorkflowDefaultActionView> actions = List.class.cast(ev.getEntity());
        for(WorkflowDefaultActionView av:actions){
          assertTrue(av.getScheme().isSystem());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void Find_Available_Default_Actions_No_Read_Permission_Invalid_License()
            throws Exception {

        ContentType contentType = null;
        final String ctName = "KeepWfTaskStatus" + System.currentTimeMillis();
        try {
            //Bill does NOT have Read permissions over this new content type
            contentType = createContentTypeAndAssignPermissions(ctName,
                    BaseContentType.CONTENT, editPermission, publisher.getId());

            final HttpServletRequest request = mock(HttpServletRequest.class);
            final Response findResponse = nonLicenseWorkflowResource
                    .findAvailableDefaultActionsByContentType(request, contentType.id());
            assertEquals(Status.UNAUTHORIZED.getStatusCode(), findResponse.getStatus());
        } finally {
            if(contentType != null){
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void Find_Initial_Available_Default_Actions_Invalid_License() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final ContentType defaultContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).findDefault();
        final Response response = nonLicenseWorkflowResource.findInitialAvailableActionsByContentType(request, defaultContentType.id());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        ResponseEntityView ev = ResponseEntityView.class.cast(response.getEntity());
        List<WorkflowDefaultActionView> actions = List.class.cast(ev.getEntity());
        for(WorkflowDefaultActionView av:actions){
            assertTrue(av.getScheme().isSystem());
        }
    }

    @Test
    public void Find_Initial_Available_Default_Actions_No_Read_Permission_Invalid_License()
            throws Exception {

        ContentType contentType = null;
        final String ctName = "KeepWfTaskStatus" + System.currentTimeMillis();
        try {
            //Bill does NOT have Read permissions over this new content type
            contentType = createContentTypeAndAssignPermissions(ctName,
                    BaseContentType.CONTENT, editPermission, publisher.getId());

            final HttpServletRequest request = mock(HttpServletRequest.class);
            final Response findResponse = nonLicenseWorkflowResource
                    .findInitialAvailableActionsByContentType(request, contentType.id());
            assertEquals(Status.UNAUTHORIZED.getStatusCode(), findResponse.getStatus());
        } finally {
            if(contentType != null){
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
            }
        }
    }
}
