package com.dotcms.rest.api.v1.workflow;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.jboss.util.Strings;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.form.WorkflowStepAddForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkflowResourceIntegrationTest {

    private static WorkflowAPI workflowAPI = null;
    private static ContentletAPI contentletAPI = null;
    private static ContentTypeAPI contentTypeAPI;
    private static ContentType type = null;
    private static RoleAPI roleAPI = null;
    private static ContentHelper contentHelper;
    private static ResponseUtil responseUtil;
    private static WorkflowHelper workflowHelper;
    private static WorkflowImportExportUtil workflowImportExportUtil;
    private static WorkflowResource workflowResource;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        contentletAPI = APILocator.getContentletAPI();
        roleAPI = APILocator.getRoleAPI();
        contentHelper = ContentHelper.getInstance();
        workflowHelper = new WorkflowHelper(workflowAPI, roleAPI, contentletAPI);
        responseUtil = ResponseUtil.INSTANCE;
        workflowImportExportUtil = WorkflowImportExportUtil.getInstance();
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("dotcms.org.1");
        when(user.getEmailAddress()).thenReturn("admin@dotcms.com");
        when(user.getFullName()).thenReturn("User Admin");

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource.init(anyString(), anyBoolean(), any(HttpServletRequest.class), anyBoolean(), anyString())).thenReturn(dataObject);

        workflowResource = new WorkflowResource(workflowHelper, contentHelper, workflowAPI, contentletAPI, responseUtil, workflowImportExportUtil, webResource);
    }

    @Test
    public void testAddSchemaThenFindIt(){
        final String randomSchemaName = RandomStringUtils.random(20, true, true);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowSchemeForm form = new WorkflowSchemeForm.Builder().schemeName(randomSchemaName).schemeDescription("").schemeArchived(false).build();
        final Response saveResponse = workflowResource.save(request,form);
        assertEquals(Response.Status.OK.getStatusCode(), saveResponse.getStatus());
        final ResponseEntityView savedEv = ResponseEntityView.class.cast(saveResponse.getEntity());
        WorkflowScheme savedScheme = WorkflowScheme.class.cast(savedEv.getEntity());
        assertNotNull(savedScheme);
        final Response findResponse = workflowResource.findSchemes(request, null);
        final ResponseEntityView listEv = ResponseEntityView.class.cast(findResponse.getEntity());
        final List<WorkflowScheme> schemaList = List.class.cast(listEv.getEntity());
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
    public void testAddStep(){

        final String randomSchemaName = "scheme-" + RandomStringUtils.random(20, true, true);
        final HttpServletRequest saveSchemeRequest = mock(HttpServletRequest.class);
        final WorkflowSchemeForm formScheme = new WorkflowSchemeForm.Builder().schemeName(randomSchemaName).schemeDescription("").schemeArchived(false).build();
        final Response saveSchemeResponse = workflowResource.save(saveSchemeRequest,formScheme);
        assertEquals(Response.Status.OK.getStatusCode(), saveSchemeResponse.getStatus());
        final ResponseEntityView savedSchemeEntityView = ResponseEntityView.class.cast(saveSchemeResponse.getEntity());
        final WorkflowScheme savedScheme = WorkflowScheme.class.cast(savedSchemeEntityView.getEntity());
        assertNotNull(savedScheme);

        final List<WorkflowStep> workflowSteps = new ArrayList<>(2);

        final String randomStepName = "step-" + RandomStringUtils.random(20, true, true);
        final HttpServletRequest addStepRequest = mock(HttpServletRequest.class);
        final WorkflowStepAddForm workflowStepAddForm = new WorkflowStepAddForm.Builder().stepName(randomStepName).schemeId(savedScheme.getId()).enableEscalation(false).escalationTime("0").escalationAction("").stepResolved(false).build();
        final Response addStepResponse = workflowResource.addStep(addStepRequest,workflowStepAddForm);
        final ResponseEntityView savedStepEntityView = ResponseEntityView.class.cast(addStepResponse.getEntity());
        final WorkflowStep workflowStep = WorkflowStep.class.cast(savedStepEntityView.getEntity());
        assertNotNull(workflowStep);
        workflowSteps.add(workflowStep);

        for(final WorkflowStep ws:workflowSteps) {
            final String randomActionName = "action-" + RandomStringUtils.random(20, true, true);
            final HttpServletRequest saveActionRequest = mock(HttpServletRequest.class);
            final Set<WorkflowState> states = WorkflowState.toSet(WorkflowState.values());
            final WorkflowActionForm form = new WorkflowActionForm.Builder().schemeId(savedScheme.getId()).
                    actionName(randomActionName).showOn(states).actionNextStep("currentStep").
                    actionAssignable(false).actionCommentable(false).requiresCheckout(false).actionNextAssign(ws.getId()).
                    //whoCanUse(Arrays.asList("")).actionCondition("").
                    build();
            final Response saveActionResponse = workflowResource.save(saveActionRequest, form);
            assertEquals(Response.Status.OK.getStatusCode(), saveActionResponse.getStatus());
            final ResponseEntityView savedActionEv = ResponseEntityView.class.cast(saveActionResponse.getEntity());
            final WorkflowAction savedAction = WorkflowAction.class.cast(savedActionEv.getEntity());
            assertNotNull(savedAction);
        }


    }

}
