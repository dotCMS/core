package com.dotcms.rest.api.v1.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.Test;

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
        new WorkflowSchemeForm.Builder().schemeName(StringPool.BLANK).build();
    }

    private WorkflowScheme toWorkflowScheme(final WorkflowSchemeForm workflowSchemeForm) {

        final WorkflowScheme scheme = new WorkflowScheme();
        scheme.setName(workflowSchemeForm.getSchemeName());
        scheme.setDescription(workflowSchemeForm.getSchemeDescription());
        return scheme;
    }

    @SuppressWarnings("unchecked")
    private WorkflowResource mockWorkflowResource(final int throwWorkflowAPIException, final WorkflowSchemeForm workflowSchemeForm) throws Exception{
        final User user = new User();
        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final WorkflowAPI workflowAPI = mock(WorkflowAPI.class);
        if(throwWorkflowAPIException == 0){
          when(workflowAPI.findScheme(anyString())).thenReturn(new WorkflowScheme ());
        } else {
            when(workflowAPI.findScheme(anyString())).thenThrow(DoesNotExistException.class);
            doThrow(new AlreadyExistException("Error Saving workflow")).when(workflowAPI).saveScheme(any(WorkflowScheme.class), eq(user));
        }

        final RoleAPI roleAPI = mock(RoleAPI.class);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);

        //final WorkflowHelper workflowHelper = new WorkflowHelper(workflowAPI, roleAPI, contentletAPI, permissionAPI, WorkflowImportExportUtil.getInstance());
        final WorkflowHelper workflowHelper = mock(WorkflowHelper.class);
        final ContentHelper contentHelper = mock(ContentHelper.class);
        final SystemActionApiFireCommandFactory systemActionApiFireCommandFactory =
                mock(SystemActionApiFireCommandFactory.class);
        final WebResource webResource = mock(WebResource.class);

        if(throwWorkflowAPIException == 0) {
            when(workflowHelper.saveOrUpdate(nullable(String.class), any(WorkflowSchemeForm.class), eq(user))).thenReturn(this.toWorkflowScheme(workflowSchemeForm));
        } else if(throwWorkflowAPIException == 1) {
            doThrow(new DoesNotExistException("No exists workflow")).when(workflowHelper).saveOrUpdate(nullable(String.class), any(WorkflowSchemeForm.class), eq(user));
        } else {
            doThrow(new AlreadyExistException("Error Saving workflow")).when(workflowHelper).saveOrUpdate(nullable(String.class), any(WorkflowSchemeForm.class), eq(user));
        }


        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource.init(nullable(String.class), any(HttpServletRequest.class), any(HttpServletResponse.class), anyBoolean(), nullable(String.class))).thenReturn(dataObject);

        final ResponseUtil responseUtil = mock(ResponseUtil.class);
        final WorkflowImportExportUtil exportUtil = mock(WorkflowImportExportUtil.class);
        final FileAssetAPI fileAssetAPI = mock(FileAssetAPI.class);
        return new WorkflowResource(workflowHelper, contentHelper, workflowAPI, contentletAPI, responseUtil,
                permissionAPI, exportUtil,new MultiPartUtils(fileAssetAPI), webResource, systemActionApiFireCommandFactory);
    }

    @Test
    public void testSaveScheme() throws Exception {
        final String uniqueSchemaName = "anyUniqueSchemaName";
        final String anyDesc = "anyDesc";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final WorkflowSchemeForm workflowSchemeForm = new WorkflowSchemeForm.Builder().schemeArchived(false).schemeDescription(anyDesc).schemeName(uniqueSchemaName).build();
        final WorkflowResource workflowResource = mockWorkflowResource(0, workflowSchemeForm);
        final Response response = workflowResource
                .saveScheme(request, httpServletResponse, workflowSchemeForm);
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
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final WorkflowSchemeForm workflowSchemeForm = new WorkflowSchemeForm.Builder().schemeArchived(true).schemeDescription(anyDesc).schemeName(uniqueSchemaName).build();
        final WorkflowResource workflowResource = mockWorkflowResource(0, workflowSchemeForm);
        final Response response = workflowResource
                .updateScheme(request, httpServletResponse, anyRandomSchemeId, workflowSchemeForm);
        final ResponseEntityView entityView = ResponseEntityView.class.cast(response.getEntity());
        assertNotNull(entityView);
        final WorkflowScheme scheme = WorkflowScheme.class.cast(entityView.getEntity());
        assertNotNull(scheme);
        assertEquals(scheme.getName(),uniqueSchemaName);
        assertEquals(scheme.getDescription(),anyDesc);
    }

    @Test
    public void testSaveExistingSchemeDupeName() throws Exception {
        final String uniqueSchemaName = "anyUniqueSchemaName";
        final String anyDesc = "anyDesc";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final WorkflowSchemeForm workflowSchemeForm = new WorkflowSchemeForm.Builder().schemeArchived(true).schemeDescription(anyDesc).schemeName(uniqueSchemaName).build();
        final WorkflowResource workflowResource = mockWorkflowResource(2, workflowSchemeForm);
        final Response response = workflowResource
                .saveScheme(request, httpServletResponse, workflowSchemeForm);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdateNonExistingScheme() throws Exception {
        final String uniqueSchemaName = "anyUniqueSchemaName";
        final String anyRandomSchemeId = "3a5f1da7-c304-4f2b-867a-df3ef3578955";
        final String anyDesc = "anyDesc";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final WorkflowSchemeForm workflowSchemeForm = new WorkflowSchemeForm.Builder().schemeArchived(true).schemeDescription(anyDesc).schemeName(uniqueSchemaName).build();
        final WorkflowResource workflowResource = mockWorkflowResource(1, workflowSchemeForm);
        final Response response = workflowResource
                .updateScheme(request, httpServletResponse, anyRandomSchemeId, workflowSchemeForm);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

}
