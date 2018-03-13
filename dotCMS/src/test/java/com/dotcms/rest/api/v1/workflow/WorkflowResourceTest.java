package com.dotcms.rest.api.v1.workflow;

import com.dotcms.UnitTestBase;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.jboss.util.Strings;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.liferay.portal.model.User;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class WorkflowResourceTest extends UnitTestBase {

    @Test(expected = ValidationException.class)
    public void testSchemaEmptyParameters() {
        new WorkflowSchemeForm.Builder().build();
    }

    @Test(expected = ValidationException.class)
    public void testNullSchemaParameters() {
        new WorkflowSchemeForm.Builder().schemeName(null).build();
    }

    @Test(expected = ValidationException.class)
    public void testEmptySchemaParameters() {
        new WorkflowSchemeForm.Builder().schemeName(Strings.EMPTY).build();
    }

    @SuppressWarnings("unchecked")
    private WorkflowResource mockWorkflowResource(final boolean throwWorkflowAPIException) throws Exception{
        final User user = new User();
        final WorkflowAPI workflowAPI = mock(WorkflowAPI.class);
        if(!throwWorkflowAPIException){
          when(workflowAPI.findScheme(anyString())).thenReturn(new WorkflowScheme ());
        } else {
            when(workflowAPI.findScheme(anyString())).thenThrow(DoesNotExistException.class);
            doThrow(new AlreadyExistException("Error Saving workflow")).when(workflowAPI).saveScheme(any(WorkflowScheme.class), eq(user));
        }

        final RoleAPI roleAPI = mock(RoleAPI.class);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);

        final WorkflowHelper workflowHelper = new WorkflowHelper(workflowAPI, roleAPI, contentletAPI);
        final ContentHelper contentHelper = mock(ContentHelper.class) ;
        final WebResource webResource = mock(WebResource.class);

        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource.init(anyString(), anyBoolean(), any(HttpServletRequest.class), anyBoolean(), anyString())).thenReturn(dataObject);

        final ResponseUtil responseUtil = mock(ResponseUtil.class);
        final WorkflowImportExportUtil exportUtil = mock(WorkflowImportExportUtil.class);
        return new WorkflowResource(workflowHelper, contentHelper, workflowAPI, contentletAPI, responseUtil, exportUtil, webResource);
    }

    @Test
    public void testSaveScheme() throws Exception {
        final String uniqueSchemaName = "anyUniqueSchemaName";
        final String anyDesc = "anyDesc";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowResource workflowResource = mockWorkflowResource(false);
        final WorkflowSchemeForm workflowSchemeForm = new WorkflowSchemeForm.Builder().schemeArchived(false).schemeDescription(anyDesc).schemeName(uniqueSchemaName).build();
        final Response response = workflowResource.save(request, workflowSchemeForm);
        final ResponseEntityView entityView = ResponseEntityView.class.cast(response.getEntity());
        assertNotNull(entityView);
        final WorkflowScheme scheme = WorkflowScheme.class.cast(entityView.getEntity());
        assertNotNull(scheme);
        assertEquals(scheme.getName(),uniqueSchemaName);
    }

    @Test
    public void testUpdateScheme() throws Exception {
        final String uniqueSchemaName = "anyUniqueSchemaName";
        final String anyRandomSchemeId = "3a5f1da7-c304-4f2b-867a-df3ef3578955";
        final String anyDesc = "anyDesc";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowResource workflowResource = mockWorkflowResource(false);
        final WorkflowSchemeForm workflowSchemeForm = new WorkflowSchemeForm.Builder().schemeArchived(true).schemeDescription(anyDesc).schemeName(uniqueSchemaName).build();
        final Response response = workflowResource.update(request, anyRandomSchemeId, workflowSchemeForm);
        final ResponseEntityView entityView = ResponseEntityView.class.cast(response.getEntity());
        assertNotNull(entityView);
        final WorkflowScheme scheme = WorkflowScheme.class.cast(entityView.getEntity());
        assertNotNull(scheme);
        assertEquals(scheme.getName(),uniqueSchemaName);
        assertEquals(scheme.getDescription(),anyDesc);
    }

    @Test
    public void testSaveNonExistingScheme() throws Exception {
        final String uniqueSchemaName = "anyUniqueSchemaName";
        final String anyDesc = "anyDesc";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowResource workflowResource = mockWorkflowResource(true);
        final WorkflowSchemeForm workflowSchemeForm = new WorkflowSchemeForm.Builder().schemeArchived(true).schemeDescription(anyDesc).schemeName(uniqueSchemaName).build();
        final Response response = workflowResource.save(request, workflowSchemeForm);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdateNonExistingScheme() throws Exception {
        final String uniqueSchemaName = "anyUniqueSchemaName";
        final String anyRandomSchemeId = "3a5f1da7-c304-4f2b-867a-df3ef3578955";
        final String anyDesc = "anyDesc";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowResource workflowResource = mockWorkflowResource(true);
        final WorkflowSchemeForm workflowSchemeForm = new WorkflowSchemeForm.Builder().schemeArchived(true).schemeDescription(anyDesc).schemeName(uniqueSchemaName).build();
        final Response response = workflowResource.update(request, anyRandomSchemeId, workflowSchemeForm);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

}
