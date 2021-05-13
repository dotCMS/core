package com.dotcms.rest.api.v1.versionable;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
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
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.ettrema.cache.Cache;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import java.util.Collections;
import java.util.List;
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

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a container, this will create an old version of,
     *                  using the endpoint find the version (the old one).
     * ExpectedResult: The endpoint should return 200. Check the versionableView against the container.
     *
     */
    @Test
    public void test_findVersion_container_success() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create container and a new version
        Container container = new ContainerDataGen().site(newHost).nextPersisted();
        final String oldContainerInode = container.getInode();
        container.setInode(UUIDGenerator.generateUuid());
        container = APILocator.getContainerAPI().save(container, Collections.emptyList(),newHost,adminUser,false);
        //Call Resource
        final Response responseResource = resource.findVersionable(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                oldContainerInode);
        //Check that the response is 200
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView entityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final VersionableView versionableView = VersionableView.class.cast(entityView.getEntity());
        Assert.assertEquals(oldContainerInode,versionableView.getInode());
        Assert.assertEquals(container.getVersionType(),versionableView.getVersionType());
        Assert.assertEquals(container.getTitle(),versionableView.getTitle());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a link, this will create an old version of,
     *                  using the endpoint find the version (the old one).
     * ExpectedResult: The endpoint should return 200. Check the versionableView against the link.
     *
     */
    @Test
    public void test_findVersion_link_success() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create container and a new version
        Link link = new LinkDataGen(new FolderDataGen().site(newHost).nextPersisted()).nextPersisted();
        final String oldLinkInode = link.getInode();
        link.setInode(UUIDGenerator.generateUuid());
        APILocator.getMenuLinkAPI().save(link,adminUser,false);
        //Call Resource
        final Response responseResource = resource.findVersionable(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                oldLinkInode);
        //Check that the response is 200
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView entityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final VersionableView versionableView = VersionableView.class.cast(entityView.getEntity());
        Assert.assertEquals(oldLinkInode,versionableView.getInode());
        Assert.assertEquals(link.getVersionType(),versionableView.getVersionType());
        Assert.assertEquals(link.getTitle(),versionableView.getTitle());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a contentlet, this will create an old version of,
     *                  using the endpoint find the version (the old one).
     * ExpectedResult: The endpoint should return 200. Check the versionableView against the contentlet.
     *
     */
    @Test
    public void test_findVersion_contentlet_success() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create contentlet and a new version
        Contentlet contentlet = TestDataUtils.getGenericContentContent(true,1,newHost);
        final String oldContentletInode = contentlet.getInode();
        contentlet.setInode(UUIDGenerator.generateUuid());
        contentlet = APILocator.getContentletAPI().checkin(contentlet,adminUser,false);
        //Call Resource
        final Response responseResource = resource.findVersionable(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                oldContentletInode);
        //Check that the response is 200
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView entityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final VersionableView versionableView = VersionableView.class.cast(entityView.getEntity());
        Assert.assertEquals(oldContentletInode,versionableView.getInode());
        Assert.assertEquals(contentlet.getVersionType(),versionableView.getVersionType());
        Assert.assertEquals(contentlet.getTitle(),versionableView.getTitle());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a template, this will create an old version of,
     *                  using the endpoint find the version (the old one).
     * ExpectedResult: The endpoint should return 200. Check the versionableView against the template.
     *
     */
    @Test
    public void test_findVersion_template_success() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template and a new version
        Template template = new TemplateDataGen().site(newHost).nextPersisted();
        final String oldTemplateInode = template.getInode();
        template.setInode(UUIDGenerator.generateUuid());
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource
        final Response responseResource = resource.findVersionable(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                oldTemplateInode);
        //Check that the response is 200
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView entityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final VersionableView versionableView = VersionableView.class.cast(entityView.getEntity());
        Assert.assertEquals(template.getVersionType(),versionableView.getVersionType());
        Assert.assertEquals(template.getTitle(),versionableView.getTitle());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a container, this will create an old version of,
     *                   using the endpoint find the version (the old one) but as a limited user.
     * ExpectedResult: The endpoint should return 403, because the user needs View/Read Permission to
     *                   find a version.
     *
     */
    @Test (expected = DotSecurityException.class)
    public void test_findVersion_limitedUserWithoutViewPermission_container_return403() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create container and a new version
        final Container container = new ContainerDataGen().site(newHost).nextPersisted();
        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Call Resource
        resource.findVersionable(getHttpRequest(limitedUser.getEmailAddress(),"admin"),response,
                container.getInode());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a contentlet, this will create an old version of,
     *                   using the endpoint find the version (the old one) but as a limited user.
     * ExpectedResult: The endpoint should return 403, because the user needs View/Read Permission to
     *                   find a version.
     *
     */
    @Test (expected = DotSecurityException.class)
    public void test_findVersion_limitedUserWithoutViewPermission_contentlet_return403() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create contentlet and a new version
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true,1,newHost);
        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Call Resource
        resource.findVersionable(getHttpRequest(limitedUser.getEmailAddress(),"admin"),response,
                contentlet.getInode());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a template, this will create an old version of,
     *                   using the endpoint find the version (the old one) but as a limited user.
     * ExpectedResult: The endpoint should return 403, because the user needs View/Read Permission to
     *                   find a version.
     *
     */
    @Test (expected = DotSecurityException.class)
    public void test_findVersion_limitedUserWithoutViewPermission_template_return403() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template and a new version
        final Template template = new TemplateDataGen().site(newHost).nextPersisted();
        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Call Resource
        resource.findVersionable(getHttpRequest(limitedUser.getEmailAddress(),"admin"),response,
                template.getInode());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: find a version of a non-existent uuid
     * ExpectedResult: The endpoint should return 404, because the uuid does not belong to any element.
     *
     */
    @Test(expected = DoesNotExistException.class)
    public void test_findVersion_inodeDoesNotExist_return404()
            throws DotSecurityException, DotDataException {
        //Call Resource
        resource.findVersionable(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                UUIDGenerator.generateUuid());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a container, this will create an old version of,
     *                  using the endpoint find all the versions.
     * ExpectedResult: The endpoint should return 200.
     *
     */
    @Test
    public void test_findAllVersions_container_success() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create container and a new version
        Container container = new ContainerDataGen().site(newHost).nextPersisted();
        container.setInode(UUIDGenerator.generateUuid());
        container = APILocator.getContainerAPI().save(container, Collections.emptyList(),newHost,adminUser,false);
        //Call Resource
        final Response responseResource = resource.findVersionable(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                container.getIdentifier());
        //Check that the response is 200
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView entityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final List<VersionableView> allVersions = List.class.cast(entityView.getEntity());
        Assert.assertEquals(2,allVersions.size());
        Assert.assertEquals(container.getTitle(),allVersions.get(0).getTitle());
        Assert.assertEquals(container.getVersionType(),allVersions.get(1).getVersionType());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a link, this will create an old version of,
     *                  using the endpoint find all the versions.
     * ExpectedResult: The endpoint should return 200.
     *
     */
    @Test
    public void test_findAllVersions_link_success() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create container and a new version
        Link link = new LinkDataGen(new FolderDataGen().site(newHost).nextPersisted()).nextPersisted();
        link.setInode(UUIDGenerator.generateUuid());
        APILocator.getMenuLinkAPI().save(link,adminUser,false);
        //Call Resource
        final Response responseResource = resource.findVersionable(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                link.getIdentifier());
        //Check that the response is 200
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView entityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final List<VersionableView> allVersions = List.class.cast(entityView.getEntity());
        Assert.assertEquals(2,allVersions.size());
        Assert.assertEquals(link.getTitle(),allVersions.get(0).getTitle());
        Assert.assertEquals(link.getVersionType(),allVersions.get(1).getVersionType());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a contentlet, this will create an old version of,
     *                  using the endpoint find all the versions.
     * ExpectedResult: The endpoint should return 200.
     *
     */
    @Test
    public void test_findAllVersions_contentlet_success() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create contentlet and a new version
        Contentlet contentlet = TestDataUtils.getGenericContentContent(true,1,newHost);
        contentlet.setInode(UUIDGenerator.generateUuid());
        contentlet = APILocator.getContentletAPI().checkin(contentlet,adminUser,false);
        //Call Resource
        final Response responseResource = resource.findVersionable(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                contentlet.getIdentifier());
        //Check that the response is 200
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView entityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final List<VersionableView> allVersions = List.class.cast(entityView.getEntity());
        Assert.assertEquals(2,allVersions.size());
        Assert.assertEquals(contentlet.getTitle(),allVersions.get(0).getTitle());
        Assert.assertEquals(contentlet.getVersionType(),allVersions.get(1).getVersionType());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a template, this will create an old version of,
     *                  using the endpoint find all the versions.
     * ExpectedResult: The endpoint should return 200.
     *
     */
    @Test
    public void test_findAllVersions_template_success() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template and a new version
        Template template = new TemplateDataGen().site(newHost).nextPersisted();
        template.setInode(UUIDGenerator.generateUuid());
        template = APILocator.getTemplateAPI().saveTemplate(template,newHost,adminUser,false);
        //Call Resource
        final Response responseResource = resource.findVersionable(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                template.getIdentifier());
        //Check that the response is 200
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView entityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final List<VersionableView> allVersions = List.class.cast(entityView.getEntity());
        Assert.assertEquals(2,allVersions.size());
        Assert.assertEquals(template.getTitle(),allVersions.get(0).getTitle());
        Assert.assertEquals(template.getVersionType(),allVersions.get(1).getVersionType());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a contentlet, this will create an old version of,
     *                   using the endpoint find all the versions but as a limited user.
     * ExpectedResult: The endpoint should return 403, because the user needs View/Read Permission to
     *                   find all versions.
     *
     */
    @Test (expected = DotSecurityException.class)
    public void test_findAllVersions_limitedUserWithoutViewPermission_contentlet_return403() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create contentlet and a new version
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true,1,newHost);
        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Call Resource
        resource.findVersionable(getHttpRequest(limitedUser.getEmailAddress(),"admin"),response,
                contentlet.getIdentifier());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: Create and edit a template, this will create an old version of,
     *                   using the endpoint find all the versions but as a limited user.
     * ExpectedResult: The endpoint should return 403, because the user needs View/Read Permission to
     *                   find all versions.
     *
     */
    @Test (expected = DotSecurityException.class)
    public void test_findAllVersions_limitedUserWithoutViewPermission_template_return403() throws DotSecurityException, DotDataException {
        final Host newHost = new SiteDataGen().nextPersisted();
        //Create template and a new version
        final Template template = new TemplateDataGen().site(newHost).nextPersisted();
        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);
        //Call Resource
        resource.findVersionable(getHttpRequest(limitedUser.getEmailAddress(),"admin"),response,
                template.getIdentifier());
    }

    /**
     * Method to test: findVersion in the VersionableResource
     * Given Scenario: find a version of a non-existent uuid
     * ExpectedResult: The endpoint should return 404, because the uuid does not belong to any element.
     *
     */
    @Test(expected = DoesNotExistException.class)
    public void test_findAllVersion_identifierDoesNotExist_return404()
            throws DotSecurityException, DotDataException {
        //Call Resource
        resource.findVersionable(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                UUIDGenerator.generateUuid());
    }
}
