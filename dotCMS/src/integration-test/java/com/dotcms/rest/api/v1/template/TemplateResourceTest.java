package com.dotcms.rest.api.v1.template;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.HttpRequestUtil;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.ThemeDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPI;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.BulkResultView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContext;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TemplateResourceTest {

    static TemplateResource resource;
    static HttpServletResponse response;
    static User adminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        resource = new TemplateResource();
        adminUser = TestUserUtils.getAdminUser();
        response = new MockHttpResponse();
    }

    /**
     * Method to test: archive in the TemplateResource
     * Given Scenario: Create a template on working state, and try to archive it.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template
     *
     */
    @Test
    public void test_archiveTemplate_templateIsWorking_success()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource
        final Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
    }

    /**
     * Method to test: archive in the TemplateResource
     * Given Scenario: Create a template on live state, and try to archive it.
     *                  Should fail since the template needs to be unpublished first.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_archiveTemplate_templateIsLive_failedToArchive() {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource
        final Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: archive in the TemplateResource
     * Given Scenario: Create a template and archive it. Then try again the archive endpoint
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template
     *
     */
    @Test
    public void test_archiveTemplate_templateIsAlreadyArchived_success()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource
        Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(), "admin"), response,
                        new ArrayList<>(
                                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class
                .cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        //Call again the resource
        responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(), "admin"), response,
                new ArrayList<>(
                        Set.of(template.getIdentifier())));
        assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());
        responseEntityView = ResponseEntityView.class
                .cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
    }

    /**
     * Method to test: archive in the TemplateResource
     * Given Scenario: Create a template on working state and create a UUID that does not belong to a template,
     *                  and try to archive both.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template and the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_archiveTemplates_OneTemplateIdDoesNotExist_OneTemplateIdExists()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        final String uuid = UUIDGenerator.generateUuid();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource
        final Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Arrays.asList(template.getIdentifier(), uuid)));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: archive in the TemplateResource
     * Given Scenario: Create a template on working state, and as a limited user without edit permissions
     *                  over the template, try to archive it.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_archiveTemplate_LimitedUserWithoutEditPermissions_failedToArchived()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Give Permissions Over the Folder
        Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                template.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, template, APILocator.systemUser(), false);
        //Call Resource
        final Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", limitedUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Arrays.asList(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: unarchive in the TemplateResource
     * Given Scenario: Create a template and archive it. Then try to unarchive the template.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template
     *
     */
    @Test
    public void test_unarchiveTemplate_templateIsArchived_success()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource to Archive
        Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        //Call Resource to UnArchive
        responseResource = resource.unarchive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
    }

    /**
     * Method to test: unarchive in the TemplateResource
     * Given Scenario: Create a template on live state. Then try to unarchive the template.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_unarchiveTemplate_templateIsLive_failedToUnArchived() {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource to UnArchive
        final Response responseResource = resource.unarchive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: unarchive in the TemplateResource
     * Given Scenario: Create a template on working state. Then try to unarchive the template.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_unarchiveTemplate_templateIsWorking_failedToUnarchive()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource to UnArchive
        final Response responseResource = resource.unarchive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: unarchive in the TemplateResource
     * Given Scenario: Create a template on working state and archive it. Also generate a UUID that does not belong to a template.
     *                   Now, try to unarchive both ids.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template and the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_unarchiveTemplates_OneTemplateIdDoesNotExist_OneTemplateIdExists()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        final String uuid = UUIDGenerator.generateUuid();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource to Archive
        Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());
        //Call Resource to UnArchive
        responseResource = resource.unarchive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Arrays.asList(template.getIdentifier(),uuid)));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: unarchive in the TemplateResource
     * Given Scenario: Create a template on working state, and archive it (as Admin).
     *                    Then try to unarchive the template, as a limited user that does not have permissions.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_unarchiveTemplate_LimitedUserWithoutEditPermissions_failedToUnarchive()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource to Archive
        Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());

        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Give Permissions Over the Folder
        Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                template.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, template, APILocator.systemUser(), false);

        //Call Resource to UnArchive
        responseResource = resource.unarchive(HttpRequestUtil.getHttpRequest("localhost", "/", limitedUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: delete in the TemplateResource
     * Given Scenario: Create a template on working state, archive it and delete it.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template
     *
     */
    @Test
    public void test_deleteTemplate_templateIsArchived_success()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource to Archive
        Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());

        //Call Resource to Delete
        responseResource = resource.delete(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());
    }

    /**
     * Method to test: delete in the TemplateResource
     * Given Scenario: Create a template on live state, and try to delete it. Since the template is
     *                  not archived it could not be deleted.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_deleteTemplate_templateIsLive_failedToDelete(){
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource to Archive
        Response responseResource = resource.delete(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());

    }

    /**
     * Method to test: delete in the TemplateResource
     * Given Scenario: Create a template on working state and archive it. Also create a UUID that does not
     *                  belong to any template. Try to delete both.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template and the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_deleteTemplates_OneTemplateIdDoesNotExist_OneTemplateIdExists()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        final String uuid = UUIDGenerator.generateUuid();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource to Archive
        Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());

        //Call Resource to Delete
        responseResource = resource.delete(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Arrays.asList(template.getIdentifier(),uuid)));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: delete in the TemplateResource
     * Given Scenario: Create a template on working state, and archive it (as Admin). Now as a Limited User
     *                  without Edit Permissions try to delete the template.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_deleteTemplate_LimitedUserWithoutEditPermissions_failedToDelete()
            throws DotDataException, DotSecurityException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource to Archive
        Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());

        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Give Permissions Over the Folder
        Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                template.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, template, APILocator.systemUser(), false);

        //Call Resource to Delete
        responseResource = resource.delete(HttpRequestUtil.getHttpRequest("localhost", "/", limitedUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: delete in the TemplateResource
     * Given Scenario: Create a template on live state, a page that uses that template. Then archive the
     *                  template and try to delete it. Since it still has dependencies should fail.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_deleteTemplate_pageStillReferencingTemplate_failedToDelete()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Create a page that uses that template
        final HTMLPageAsset page = new HTMLPageDataGen(newHost,template).nextPersisted();
        //Call Resource to Archive
        Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());
        //Call Resource to Delete
        responseResource = resource.delete(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());

    }

    /**
     * Method to test: publish in the TemplateResource
     * Given Scenario: Create a template on working state, and publish it.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template
     *
     */
    @Test
    public void test_publishTemplate_success() throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource
        final Response responseResource = resource.publish(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());
    }

    /**
     * Method to test: publish in the TemplateResource
     * Given Scenario: Create a template on working state, and publish it. Now try to
     *                  publish it again.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template
     *
     */
    @Test
    public void test_publishTemplate_templateIsAlreadyPublished_success()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource
        Response responseResource = resource.publish(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());

        //Call Resource again
        responseResource = resource.publish(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());
    }

    /**
     * Method to test: publish in the TemplateResource
     * Given Scenario: Create a template on working state, and archive it. Since the template is
     *                  archived it could not be published.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_publishTemplate_templateIsArchived_failedToPublish()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource
        Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());

        //Call Resource
        responseResource = resource.publish(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: publish in the TemplateResource
     * Given Scenario: Create a template on working state. Also create a UUID that does not
     *                  belong to any template. Try to publish both.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template and the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_publishTemplate_OneTemplateIdDoesNotExist_OneTemplateIdExists()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        final String uuid = UUIDGenerator.generateUuid();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource
        final Response responseResource = resource.publish(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Arrays.asList(template.getIdentifier(), uuid)));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: publish in the TemplateResource
     * Given Scenario: Create a template on working state. Now as a Limited User
     *                  without Publish Permissions try to publish the template.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_publishTemplate_LimitedUserWithoutPublishPermissions_failedToPublish()
            throws DotDataException, DotSecurityException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Give Permissions Over the Folder
        Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                template.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, template, APILocator.systemUser(), false);
        //Call Resource
        final Response responseResource = resource.publish(HttpRequestUtil.getHttpRequest("localhost", "/", limitedUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Arrays.asList(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: unpublish in the TemplateResource
     * Given Scenario: Create a template on live state, and unpublish it.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template
     *
     */
    @Test
    public void test_unpublishTemplate_success() {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource
        final Response responseResource = resource.unpublish(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());
    }

    /**
     * Method to test: unpublish in the TemplateResource
     * Given Scenario: Create a template on live state, and unpublish it. Now try to
     *                  unpublish it again.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template
     *
     */
    @Test
    public void test_unpublishTemplate_templateIsAlreadyUnpublished_success() {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource
        Response responseResource = resource.unpublish(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());

        //Call Resource again
        responseResource = resource.unpublish(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());
    }

    /**
     * Method to test: unpublish in the TemplateResource
     * Given Scenario: Create a template on working state, and archive it. Now try to unpublish,
     *                  since the template is archived it could not be unpublished.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_unpublishTemplate_templateIsArchived_failedToUnpublish()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource
        Response responseResource = resource.archive(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(0,results.getFails().size());

        //Call Resource
        responseResource = resource.unpublish(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Set.of(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: unpublish in the TemplateResource
     * Given Scenario: Create a template on live state. Also create a UUID that does not
     *                  belong to any template. Try to unpublish both.
     * ExpectedResult: The endpoint should return 200, the successCount must be 1 because
     *                  the action was executed successfully over 1 template and the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_unpublishTemplate_OneTemplateIdDoesNotExist_OneTemplateIdExists(){
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        final String uuid = UUIDGenerator.generateUuid();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource
        final Response responseResource = resource.unpublish(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Arrays.asList(template.getIdentifier(), uuid)));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(1L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: unpublish in the TemplateResource
     * Given Scenario: Create a template on live state. Now as a Limited User
     *                  without Edit Permissions try to unpublish the template.
     * ExpectedResult: The endpoint should return 200, the failed array size
     *                   must be 1 because the action failed over 1 template
     *
     */
    @Test
    public void test_unpublishTemplate_LimitedUserWithoutEditPermissions_failedToPublish()
            throws DotDataException, DotSecurityException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Give Permissions Over the Folder
        Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                template.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, template, APILocator.systemUser(), false);
        //Call Resource
        final Response responseResource = resource.unpublish(HttpRequestUtil.getHttpRequest("localhost", "/", limitedUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Arrays.asList(template.getIdentifier())));
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final BulkResultView results = BulkResultView.class.cast(responseEntityView.getEntity());
        assertEquals(java.util.Optional.of(0L).get(),results.getSuccessCount());
        assertEquals(1,results.getFails().size());
    }

    /**
     * Method to test: copy in the TemplateResource
     * Given Scenario: Create a template on working state, and try to copy it.
     * ExpectedResult: The endpoint should return 200 and the new template info should
     *                 be in the response.
     *
     */
    @Test
    public void test_copyTemplate_success()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        Template template = new TemplateDataGen().title(title).next();
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource
        final Response responseResource = resource.copy(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,template.getIdentifier());
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final TemplateView results = TemplateView.class.cast(responseEntityView.getEntity());
        assertEquals(title + " - 1",results.getTitle());
    }

    /**
     * Method to test: copy in the TemplateResource
     * Given Scenario: Create a UUID that does not belong to any template and try to copy.
     * ExpectedResult: Should throw a DoesNotExistException
     *
     */
    @Test (expected = DoesNotExistException.class)
    public void test_copyTemplate_IdDoesNotBelongToAnyTemplate_failedToCopy()
            throws DotSecurityException, DotDataException {
        final String uuid = UUIDGenerator.generateUuid();
        //Call Resource
        resource.copy(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,uuid);
    }

    /**
     * Method to test: copy in the TemplateResource
     * Given Scenario: Create a template on live state. Now as a Limited User
     *                  without READ Permissions try to copy the template.
     * ExpectedResult: Should throw a DotSecurityException
     *
     */
    @Test (expected = DotSecurityException.class)
    public void test_copyTemplate_LimitedUserWithoutReadPermissions_failedToCopy()
            throws DotDataException, DotSecurityException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Call Resource
        resource.copy(HttpRequestUtil.getHttpRequest("localhost", "/", limitedUser.getEmailAddress(),"admin"),response,template.getIdentifier());
    }

    /**
     * Method to test: list in the TemplateResource
     * Given Scenario: Create a template on hostA, and a template on hostB. Get the templates of hostA.
     * ExpectedResult: The endpoint should return 200, and the size of the results must be 1.
     */
    @Test
    public void test_listTemplate_filterByHost()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHostA = new SiteDataGen().nextPersisted();
        final Host newHostB = new SiteDataGen().nextPersisted();
        //Create template
        Template templateA = new TemplateDataGen().title(title).next();
        templateA = APILocator.getTemplateAPI().saveTemplate(templateA,newHostA,adminUser,false);
        Template templateB = new TemplateDataGen().title(title).next();
        templateB = APILocator.getTemplateAPI().saveTemplate(templateB,newHostB,adminUser,false);
        //Call Resource
        final Response responseResource = resource.list(HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin"),response,"",0,40,"mod_date","DESC",newHostA.getIdentifier(),false);
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final PaginatedArrayList paginatedArrayList = PaginatedArrayList.class.cast(responseEntityView.getEntity());
        final PaginatedArrayList paginatedArrayListWithoutSystemTemplate =  this.removeSystemTemplateFromList(paginatedArrayList);
        assertEquals(1,paginatedArrayListWithoutSystemTemplate.size());
    }

    private PaginatedArrayList<?> removeSystemTemplateFromList(final PaginatedArrayList<?> paginatedArrayList) {

        final PaginatedArrayList paginatedArrayListWithoutSystemTemplate = new PaginatedArrayList();

        paginatedArrayListWithoutSystemTemplate.setQuery(paginatedArrayList.getQuery());
        paginatedArrayListWithoutSystemTemplate.setTotalResults(paginatedArrayList.getTotalResults());

        for(Object templateObject : paginatedArrayList) {

            if (templateObject instanceof TemplateView &&
                    !Template.SYSTEM_TEMPLATE.equals(((TemplateView) templateObject).getIdentifier())) {

                paginatedArrayListWithoutSystemTemplate.add(templateObject);
            }
        }

        return paginatedArrayListWithoutSystemTemplate;
    }

    /**
     * Method to test: list in the TemplateResource
     * Given Scenario: Create a template on hostA, and a template on hostB. Get the templates of hostA, but using the
     *                  currentHost not the query param.
     * ExpectedResult: The endpoint should return 200, and the size of the results must be 1.
     */
    @Test
    public void test_listTemplate_filterByHost_usingCurrentHost()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHostA = new SiteDataGen().nextPersisted();
        final Host newHostB = new SiteDataGen().nextPersisted();
        //Create template
        Template templateA = new TemplateDataGen().title(title).next();
        templateA = APILocator.getTemplateAPI().saveTemplate(templateA,newHostA,adminUser,false);
        Template templateB = new TemplateDataGen().title(title).next();
        templateB = APILocator.getTemplateAPI().saveTemplate(templateB,newHostB,adminUser,false);
        //Create Request and set Attribute
        final HttpServletRequest request = HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(),"admin");
        request.getSession().setAttribute(WebKeys.CURRENT_HOST,newHostA);
        //Call Resource
        final Response responseResource = resource.list(request,response,"",0,40,"mod_date","DESC","",false);
        //Check that the response is 200, OK
        assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final PaginatedArrayList paginatedArrayList = PaginatedArrayList.class.cast(responseEntityView.getEntity());
        final PaginatedArrayList paginatedArrayListWihoutSystemTemplate = removeSystemTemplateFromList(paginatedArrayList);
        assertEquals(1,paginatedArrayListWihoutSystemTemplate.size());
        assertEquals(APILocator.getIdentifierAPI().find(templateA.getIdentifier()).getHostId(),
                APILocator.getIdentifierAPI().find(((TemplateView) paginatedArrayListWihoutSystemTemplate.get(0)).getIdentifier()).getHostId());
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link TemplateResource#list(HttpServletRequest, HttpServletResponse, String, int, int, String, String, String, boolean)}</li>
     *     <li><b>Given Scenario: </b>Create 2 templates, and a limited user. Give READ
     *     Permissions to one template to the limited user. Get All the templates that the user
     *     can READ.</li>
     *     <li><b>Expected Result: </b>The endpoint should return 200, and the size of the
     *     results must be 1.</li>
     * </ul>
     */
    @Test
    public void test_listTemplate_limitedUserNoREADPermissionsOverOneTemplate()
            throws DotSecurityException, DotDataException {
        final String postfix = String.valueOf(System.currentTimeMillis());
        final Host newHostA = new SiteDataGen().name("site.for.templates-" + postfix).nextPersisted();
        final TemplateAPI templateAPI = APILocator.getTemplateAPI();
        final User SYSTEM_USER = APILocator.systemUser();

        //Create template
        final Template templateA = new TemplateDataGen().title("Template-A-" + postfix).next();
        templateAPI.saveTemplate(templateA, newHostA, adminUser, false);
        Template templateB = new TemplateDataGen().title("Template-B-" + postfix).next();
        templateB = templateAPI.saveTemplate(templateB, newHostA, adminUser, false);
        //Call Resource
        Response responseResource = resource.list(
                HttpRequestUtil.getHttpRequest("localhost", "/", adminUser.getEmailAddress(), "admin"),
                response, "", 0, 40, "mod_date", "DESC",
                newHostA.getIdentifier(), false);

        //Check that the response is 200, OK
        assertEquals(String.format("Resulting Status Code '%s' IS NOT the expected one",
                responseResource.getStatus()), Status.OK.getStatusCode(),
                responseResource.getStatus());

        ResponseEntityView<?> responseEntityView = (ResponseEntityView<?>) responseResource.getEntity();
        PaginatedArrayList<?> paginatedArrayList = (PaginatedArrayList<?>) responseEntityView.getEntity();

        int resultTemplateListSize = this.removeSystemTemplateFromList(paginatedArrayList).size();
        assertEquals(String.format("The total returned templates: '%d' IS NOT the expected " +
                "result", resultTemplateListSize), 2, resultTemplateListSize);

        //Create the limited user
        final User limitedUser = new UserDataGen().emailAddress("user-template-list" + postfix + "@dotcms.com").roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        limitedUser.setPassword("admin");
        APILocator.getUserAPI().save(limitedUser, SYSTEM_USER,false);
        //Give Permissions Over the Template B
        final Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                templateB.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, templateB, SYSTEM_USER, false);
        //Call Resource
        responseResource = resource.list(
                HttpRequestUtil.getHttpRequest("localhost", "/", limitedUser.getEmailAddress(), "admin"),
                response, "", 0, 400, "mod_date", "DESC",
                newHostA.getIdentifier(), false);

        //Check that the response is 200, OK
        assertEquals(String.format("Resulting Status Code '%s' IS NOT the expected one", responseResource.getStatus()), Status.OK.getStatusCode(), responseResource.getStatus());

        responseEntityView = (ResponseEntityView<?>) responseResource.getEntity();
        paginatedArrayList = (PaginatedArrayList<?>) responseEntityView.getEntity();

        resultTemplateListSize = this.removeSystemTemplateFromList(paginatedArrayList).size();
        assertEquals(String.format("The total returned templates: '%d' in site '%s' IS NOT the expected " +
                "result: %s", resultTemplateListSize, newHostA.getHostname(), templateData(paginatedArrayList)), 1,
                this.removeSystemTemplateFromList(paginatedArrayList).size());
    }

    private List<Map<String, String>> templateData(PaginatedArrayList list) {
        return (List<Map<String, String>>) list.stream().map(item -> {

            return Map.of("title", ((TemplateView) item).getTitle());

        }).collect(Collectors.toList());
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: You have a Page that:
     * - it is using a Template that hava a wrong Container UUID like 1562770692396
     * - it has Contentlet into it.
     *
     * And the Template is changed
     *
     * Should: Keep render the Contentlet that is inside the page.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test
    public void legacyUUIDTemplate() throws DotDataException, DotSecurityException, InterruptedException {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final String uuid = "1562770692396";

        final String legacyDrawBody = "<div id=\"resp-template\" name=\"globalContainer\">"
                    + "<div id=\"hd-template\">"
                        + "<h1>Header</h1>"
                    + "</div>"
                    + "<div id=\"bd-template\">"
                        + "<div id=\"yui-main-template\">"
                            + "<div class=\"yui-b-template\" id=\"splitBody0\">"
                                + "<div class=\"addContainerSpan\">"
                                    + "<a href=\"javascript: showAddContainerDialog('splitBody0');\" title=\"Add Container\">"
                                        + "<span class=\"plusBlueIcon\"></span>"
                                        + "Add Container"
                                    + "</a>"
                                + "</div>"
                                + "<span class=\"titleContainerSpan\" id=\"splitBody0_span_69b3d24d-7e80-4be6-b04a-d352d16493ee_1562770692396\" title=\"container_69b3d24d-7e80-4be6-b04a-d352d16493ee\">"
                                    + "<div class=\"removeDiv\">"
                                        + "<a href=\"javascript: removeDrawedContainer('splitBody0','69b3d24d-7e80-4be6-b04a-d352d16493ee','1562770692396');\" title=\"Remove Container\">"
                                            + "<span class=\"minusIcon\"></span>Remove Container"
                                        + "</a>"
                                    + "</div>"
                                    + "<div class=\"clear\"></div>"
                                    + "<h2>Container: Default</h2>"
                                    + "<p>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do "
                                        + "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris "
                                        + "nisi ut aliquip ex ea commodo consequat."
                                    + "</p>"
                                + "</span>"
                                + "<div style=\"display: none;\" title=\"container_69b3d24d-7e80-4be6-b04a-d352d16493ee\" id=\"splitBody0_div_69b3d24d-7e80-4be6-b04a-d352d16493ee_1562770692396\">"
                                    + "#parseContainer('%s','%s')\n"
                                + "</div>"
                            + "</div>"
                        + "</div>"
                    + "</div>"
                    + "<div id=\"ft-template\"><h1>Footer</h1></div>"
                + "</div>";

        final String legacyBody = "#parseContainer('%s', '%s')";

        final Field field = new FieldDataGen().type(TextField.class).next();
        final ContentType contentType = new ContentTypeDataGen().field(field).nextPersisted();
        final Container container = new ContainerDataGen()
                .withContentType(contentType, "${" + field.variable() + "}")
                .nextPersisted();

        final String legacyDrawBodyFormatted = String.format(legacyDrawBody, container.getIdentifier(), uuid);

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), "Testing")
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .drawedBody(legacyDrawBodyFormatted)
                .body(String.format(legacyBody, container.getIdentifier(), uuid))
                .theme(theme)
                .nextPersisted();

        final TemplateResource templateResource = new TemplateResource();

        final Host site = new SiteDataGen().nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(site, template)
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final MultiTree multiTree = new MultiTreeDataGen()
                .setPage(htmlPageAsset)
                .setContainer(container)
                .setContentlet(contentlet)
                .setInstanceID( uuid)
                .setTreeOrder(0)
                .nextPersisted();

        final HttpSession mockHttpSession = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("host_id")).thenReturn(site.getIdentifier());
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(APILocator.systemUser());
        when(request.getSession()).thenReturn(mockHttpSession);

        final TemplateHelper templateHelper = new TemplateHelper();
        final TemplateView templateView = templateHelper.toTemplateView(template,
                APILocator.systemUser());

        final HttpServletResponse response = mock(HttpServletResponse.class);
        final TemplateForm templateForm = new TemplateForm.Builder()
                .siteId(site.getIdentifier())
                .identifier(template.getIdentifier())
                .inode(template.getInode())
                .layout(templateView.getLayout())
                .title(template.getTitle())
                .theme(template.getTheme())
                .build();

        final PageContext pageContext =  new PageContext(APILocator.systemUser(),
                htmlPageAsset.getPageUrl(), PageMode.PREVIEW_MODE, htmlPageAsset);

        final String pageHtml_1 = APILocator.getHTMLPageAssetRenderedAPI()
                .getPageHtml(pageContext, request, response);

        assertTrue(pageHtml_1.contains("<div>Testing</div>"));

        final Template templateFromDaBaseBeforeUpdate = APILocator.getTemplateAPI()
                .findWorkingTemplate(template.getIdentifier(), APILocator.systemUser(), false);

        final TemplateLayout templateLayoutBeforeUpdate = DotTemplateTool.getTemplateLayout(
                templateFromDaBaseBeforeUpdate.getDrawedBody());
        assertTrue(templateLayoutBeforeUpdate.existsContainer(container, uuid));

        //Thread.sleep(10000);
        templateResource.save(request, response, templateForm);

        final Template templateFromDaBaseAfterUpdate = APILocator.getTemplateAPI()
                .findWorkingTemplate(template.getIdentifier(), APILocator.systemUser(), false);

        final TemplateLayout templateLayoutAfterUpdate = DotTemplateTool.getTemplateLayout(
                templateFromDaBaseAfterUpdate.getDrawedBody());
        assertTrue(templateLayoutAfterUpdate.existsContainer(container, "1"));

        System.out.println("SECOND TIME------------------------");

        final String pageHtml_2 = APILocator.getHTMLPageAssetRenderedAPI()
                .getPageHtml(pageContext, request, response);
        assertTrue(pageHtml_2.contains("<div>Testing</div>"));



    }
}
