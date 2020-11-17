package com.dotcms.rest.api.v1.template;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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

    private HttpServletRequest getHttpRequest(final String userEmail,final String password) {
        final String userEmailAndPassword = userEmail + ":" + password;
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode(userEmailAndPassword.getBytes())));


        return request;
    }

    /**
     * Method to test: archive in the TemplateResource
     * Given Scenario: Create a template on working state, and try to archive it.
     * ExpectedResult: The endpoint should return 200, the entity must contain the identifier of the
     *                  archived template (on the archivedTemplates key)
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
        final Response responseResource = resource.archive(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Collections.singleton(template.getIdentifier())));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final HashMap<String,ArrayList<String>> results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("archivedTemplates").contains(template.getIdentifier()));
    }

    /**
     * Method to test: archive in the TemplateResource
     * Given Scenario: Create a template on live state, and try to archive it.
     * ExpectedResult: The endpoint should return 200, the entity must contain the identifier of the
     *                  archived template (on the archivedTemplates key)
     *
     */
    @Test
    public void test_archiveTemplate_templateIsLive_success() {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource
        final Response responseResource = resource.archive(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Collections.singleton(template.getIdentifier())));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final HashMap<String,ArrayList<String>> results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("archivedTemplates").contains(template.getIdentifier()));
    }

    /**
     * Method to test: archive in the TemplateResource
     * Given Scenario: Create a template and archive it. Then try again the archive endpoint
     * ExpectedResult: The endpoint should return 200, the entity must contain the identifier of the
     *                  archived template(on the archivedTemplates key)
     *
     */
    @Test
    public void test_archiveTemplate_templateIsAlreadyArchived_success() {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template working
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource
        Response responseResource = resource.archive(getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                        new ArrayList<>(
                                Collections.singleton(template.getIdentifier())));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class
                .cast(responseResource.getEntity());
        HashMap<String, ArrayList<String>> results = HashMap.class
                .cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("archivedTemplates").contains(template.getIdentifier()));
        //Call again the resource
        responseResource = resource.archive(getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new ArrayList<>(
                        Collections.singleton(template.getIdentifier())));
        Assert.assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());
        responseEntityView = ResponseEntityView.class
                .cast(responseResource.getEntity());
        results = HashMap.class
                .cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("archivedTemplates").contains(template.getIdentifier()));
    }

    /**
     * Method to test: archive in the TemplateResource
     * Given Scenario: Create a template on live state and create a UUID that does not belong to a template,
     *                  and try to archive both.
     * ExpectedResult: The endpoint should return 200, the entity must contain the identifier of the
     *                  archived template (on the archivedTemplates key)  and the other uuid (on the failedToArchived key).
     *
     */
    @Test
    public void test_archiveTemplates_OneTemplateIdDoesNotExist_OneTemplateIdExists() {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        final String uuid = UUIDGenerator.generateUuid();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource
        final Response responseResource = resource.archive(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Arrays.asList(template.getIdentifier(), uuid)));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final HashMap<String,ArrayList<String>> results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("archivedTemplates").contains(template.getIdentifier()));
        Assert.assertTrue(results.get("failedToArchive").contains(uuid));
    }

    /**
     * Method to test: archive in the TemplateResource
     * Given Scenario: Create a template on live state, and as a limited user without edit permissions
     *                  over the template, try to archive it.
     * ExpectedResult: The endpoint should return 200, the entity must contain the identifier of the
     *                  archived template(on the failedToArchived key)
     *
     */
    @Test
    public void test_archiveTemplate_LimitedUserWithoutEditPermissions_failedToArchived()
            throws DotSecurityException, DotDataException {
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
        final Response responseResource = resource.archive(getHttpRequest(limitedUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Arrays.asList(template.getIdentifier())));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final HashMap<String,ArrayList<String>> results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("failedToArchive").contains(template.getIdentifier()));
    }

    /**
     * Method to test: unarchive in the TemplateResource
     * Given Scenario: Create a template and archive it. Then try to unarchive the template.
     * ExpectedResult: The endpoint should return 200, the entity must contain the identifier of the
     *                  archived template(on the unarchivedTemplates key)
     *
     */
    @Test
    public void test_unarchiveTemplate_templateIsArchived_success() {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource to Archive
        Response responseResource = resource.archive(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Collections.singleton(template.getIdentifier())));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        HashMap<String,ArrayList<String>> results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("archivedTemplates").contains(template.getIdentifier()));
        //Call Resource to UnArchive
        responseResource = resource.unarchive(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Collections.singleton(template.getIdentifier())));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("unarchivedTemplates").contains(template.getIdentifier()));
    }

    /**
     * Method to test: unarchive in the TemplateResource
     * Given Scenario: Create a template on live state. Then try to unarchive the template.
     * ExpectedResult: The endpoint should return 200, the entity must contain the identifier of the
     *                  archived template(on the failedToUnarchived key)
     *
     */
    @Test
    public void test_unarchiveTemplate_templateIsLive_failedToUnArchived() {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource to UnArchive
        final Response responseResource = resource.unarchive(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Collections.singleton(template.getIdentifier())));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final HashMap<String,ArrayList<String>> results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("failedToUnarchive").contains(template.getIdentifier()));
    }

    /**
     * Method to test: unarchive in the TemplateResource
     * Given Scenario: Create a template on working state. Then try to unarchive the template.
     * ExpectedResult: The endpoint should return 200, the entity must contain the identifier of the
     *                  archived template(on the failedToUnarchived key)
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
        final Response responseResource = resource.unarchive(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Collections.singleton(template.getIdentifier())));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final HashMap<String,ArrayList<String>> results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("failedToUnarchive").contains(template.getIdentifier()));
    }

    /**
     * Method to test: unarchive in the TemplateResource
     * Given Scenario: Create a template on live state and archive it. Also generate a UUID that does not belong to a template.
     *                   Now, try to unarchive both ids.
     * ExpectedResult: The endpoint should return 200, the entity must contain the identifier of the
     *                  archived template(on the unarchivedTemplates key) and the UUID (on the failedToUnarchive key)
     *
     */
    @Test
    public void test_unarchiveTemplates_OneTemplateIdDoesNotExist_OneTemplateIdExists(){
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        final String uuid = UUIDGenerator.generateUuid();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource to Archive
        Response responseResource = resource.archive(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Collections.singleton(template.getIdentifier())));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        HashMap<String,ArrayList<String>> results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("archivedTemplates").contains(template.getIdentifier()));
        //Call Resource to UnArchive
        responseResource = resource.unarchive(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Arrays.asList(template.getIdentifier(),uuid)));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("unarchivedTemplates").contains(template.getIdentifier()));
        Assert.assertTrue(results.get("failedToUnarchive").contains(uuid));
    }

    /**
     * Method to test: unarchive in the TemplateResource
     * Given Scenario: Create a template on live state, and archive it (as Admin).
     *                    Then try to unarchive the template, as a limited user that does not have permissions.
     * ExpectedResult: The endpoint should return 200, the entity must contain the identifier of the
     *                  archived template(on the failedToUnarchived key)
     *
     */
    @Test
    public void test_unarchiveTemplate_LimitedUserWithoutEditPermissions_failedToUnarchive()
            throws DotSecurityException, DotDataException {
        final String title = "Template" + System.currentTimeMillis();
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template
        final Template template = new TemplateDataGen().title(title).host(newHost).nextPersisted();
        //Call Resource to Archive
        Response responseResource = resource.archive(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Collections.singleton(template.getIdentifier())));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        HashMap<String,ArrayList<String>> results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("archivedTemplates").contains(template.getIdentifier()));

        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);

        //Call Resource to UnArchive
        responseResource = resource.unarchive(getHttpRequest(limitedUser.getEmailAddress(),"admin"),response,new ArrayList<>(
                Collections.singleton(template.getIdentifier())));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        results = HashMap.class.cast(responseEntityView.getEntity());
        Assert.assertTrue(results.get("failedToUnarchive").contains(template.getIdentifier()));
    }

}
