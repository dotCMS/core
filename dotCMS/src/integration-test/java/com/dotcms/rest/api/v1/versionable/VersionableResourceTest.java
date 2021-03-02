package com.dotcms.rest.api.v1.versionable;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class VersionableResourceTest {

    static VersionableResource resource;
    static HttpServletResponse response;
    static User adminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        resource = new VersionableResource();
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
     * Method to test: deleteVersion in the VersionableResource
     * Given Scenario: Create and edit a container, this will create an old version of,
     *                  using the endpoint delete the version (the old one).
     * ExpectedResult: The endpoint should return 200.
     *
     */
    @Test
    public void test_deleteVersion_container_success() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create container and a new version
        Container container = new ContainerDataGen().site(newHost).nextPersisted();
        final String oldContainerInode = container.getInode();
        container.setInode(UUIDGenerator.generateUuid());
        container = APILocator.getContainerAPI().save(container, Collections.emptyList(),newHost,adminUser,false);
        //Call Resource
        final Response responseResource = resource.deleteVersion(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                oldContainerInode);
        //Check that the response is 200
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
    }


    /**
     * Method to test: deleteVersion in the VersionableResource
     * Given Scenario: Create a container, using the endpoint delete the version (current one).
     * ExpectedResult: The endpoint should return 400, because the version you are trying to delete
     *                  is the current live or working version.
     *
     */
    @Test (expected = DotStateException.class)
    public void test_deleteVersion_versionIsWorkingOrLive_container_return400() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create container and a new version
        final Container container = new ContainerDataGen().site(newHost).nextPersisted();
        //Call Resource
        resource.deleteVersion(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                container.getInode());
    }

    /**
     * Method to test: deleteVersion in the VersionableResource
     * Given Scenario: Create and edit a container, this will create an old version of,
     *                   using the endpoint delete the version (the old one) but as a limited user
     *                   that only have view permissions.
     * ExpectedResult: The endpoint should return 403, because the user needs Edit Permission to
     *                   delete a version.
     *
     */
    @Test (expected = DotSecurityException.class)
    public void test_deleteVersion_limitedUserWithoutEditPermission_container_return403() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create container and a new version
        final Container container = new ContainerDataGen().site(newHost).nextPersisted();
        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Give Permissions Over the Container
        Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                container.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, container, APILocator.systemUser(), false);
        //Call Resource
        resource.deleteVersion(getHttpRequest(limitedUser.getEmailAddress(),"admin"),response,
                container.getInode());
    }

    /**
     * Method to test: deleteVersion in the VersionableResource
     * Given Scenario: Create and edit a contentlet, this will create an old version of,
     *                  using the endpoint delete the version (the old one).
     * ExpectedResult: The endpoint should return 200.
     *
     */
    @Test
    public void test_deleteVersion_contentlet_success() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create contentlet and a new version
        Contentlet contentlet = TestDataUtils.getGenericContentContent(true,1,newHost);
        final String oldContentletInode = contentlet.getInode();
        contentlet.setInode(UUIDGenerator.generateUuid());
        contentlet = APILocator.getContentletAPI().checkin(contentlet,adminUser,false);
        //Call Resource
        final Response responseResource = resource.deleteVersion(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                oldContentletInode);
        //Check that the response is 200
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
    }


    /**
     * Method to test: deleteVersion in the VersionableResource
     * Given Scenario: Create a contentlet, using the endpoint delete the version (current one).
     * ExpectedResult: The endpoint should return 400, because the version you are trying to delete
     *                  is the current live or working version.
     *
     */
    @Test (expected = DotStateException.class)
    public void test_deleteVersion_versionIsWorkingOrLive_contentlet_return400() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create contentlet and a new version
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true,1,newHost);
        //Call Resource
        resource.deleteVersion(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                contentlet.getInode());
    }

    /**
     * Method to test: deleteVersion in the VersionableResource
     * Given Scenario: Create and edit a contentlet, this will create an old version of,
     *                   using the endpoint delete the version (the old one) but as a limited user
     *                   that only have view permissions.
     * ExpectedResult: The endpoint should return 403, because the user needs Edit Permission to
     *                   delete a version.
     *
     */
    @Test (expected = DotSecurityException.class)
    public void test_deleteVersion_limitedUserWithoutEditPermission_contentlet_return403() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create contentlet and a new version
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true,1,newHost);
        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Give Permissions Over the Container
        Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                contentlet.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, contentlet, APILocator.systemUser(), false);
        //Call Resource
        resource.deleteVersion(getHttpRequest(limitedUser.getEmailAddress(),"admin"),response,
                contentlet.getInode());
    }
}
