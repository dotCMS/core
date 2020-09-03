package com.dotcms.rest.api.v1.folder;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
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
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.exception.InvalidFolderNameException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FolderResourceTest {

    static HttpServletResponse response;
    static FolderResource resource;
    static FolderAPI folderAPI;
    static User adminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        response = new MockHttpResponse();
        resource = new FolderResource();
        folderAPI = APILocator.getFolderAPI();
        adminUser = TestUserUtils.getAdminUser();
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
     * Method to test: createFolders in the FolderResource
     * Given Scenario: Create a few folders using the admin as the user
     * ExpectedResult: The folders should be created successfully, so a 200 code should be returned
     *
     */
    @Test
    public void test_createFolders_success() throws DotDataException, DotSecurityException {
        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test_"+currentTime+"/folder_"+currentTime,"/test2_"+currentTime+"/","test3_"+currentTime);
        final Host newHost = new SiteDataGen().nextPersisted();

        final Response responseResource = resource.createFolders(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,foldersToCreate,newHost.getHostname());

        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());

        //Check Results
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final List<Map<String,Object>> listOfResults = List.class.cast(responseEntityView.getEntity());
        for(int i=0;i<listOfResults.size();i++){
            final String identifier = listOfResults.get(i).get("identifier").toString();
            final Folder folderCreated = folderAPI.find(identifier,adminUser,false);
            //The folder name must be contained in the foldersToCreate list
            Assert.assertTrue(foldersToCreate.get(i).contains(folderCreated.getName()));
        }
    }

    /**
     * Method to test: createFolders in the FolderResource
     * Given Scenario: Try to create a few folders using an user that does not have permissions to create folders
     * ExpectedResult: The endpoint should return DotSecurityException that jersey will map to a 403 code and no folders created
     *
     */
    @Test (expected = DotSecurityException.class)
    public void test_createFolders_UserNoPermissions_return403() throws DotDataException, DotSecurityException {
        final Host newHost = new SiteDataGen().nextPersisted();
        final User chrisUser = TestUserUtils.getChrisPublisherUser(newHost);
        final String password = "admin";
        chrisUser.setPassword(password);
        APILocator.getUserAPI().save(chrisUser,APILocator.systemUser(),false);
        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test"+currentTime+"/folder"+currentTime,"/test2"+currentTime+"/","test3"+currentTime);

        resource.createFolders(getHttpRequest(chrisUser.getEmailAddress(),password),response,foldersToCreate,newHost.getHostname());

    }

    /**
     * Method to test: createFolders in the FolderResource
     * Given Scenario: Try to create a few folders using the admin user, but the siteName passed does not belong to any site
     * ExpectedResult: The endpoint should return IllegalArgumentException that jersey will map to a 400 code and no folders created
     *
     */
    @Test (expected = IllegalArgumentException.class)
    public void test_createFolders_siteNameNotExists_return400() throws DotDataException, DotSecurityException {
        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test"+currentTime+"/folder"+currentTime,"/test2"+currentTime+"/","test3"+currentTime);

        resource.createFolders(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,foldersToCreate,"siteNameNotExists");

    }

    /**
     * Method to test: createFolders in the FolderResource
     * Given Scenario: Try to create a few folders using the admin user, but the folder name passed is restricted
     * ExpectedResult: The endpoint should return InvalidFolderNameException that jersey will map to a 400 code and no folders created
     *
     */
    @Test (expected = InvalidFolderNameException.class)
    public void test_createFolders_restrictedFolderName_return400() throws DotDataException, DotSecurityException {
        final Host newHost = new SiteDataGen().nextPersisted();
        final List<String> foldersToCreate = Arrays.asList("dotcms");

        resource.createFolders(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,foldersToCreate,newHost.getHostname());

    }

    /**
     * Method to test: loadFolderAndSubFoldersByPath in the FolderResource
     * Given Scenario: Create a few folders/subfolders and get them using the admin as the user
     * ExpectedResult: All the folders/subfolders of the requested folder path, 200 code.
     *
     */
    @Test
    public void test_loadFolderAndSubFoldersByPath_Admin_success() throws DotDataException, DotSecurityException {
        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test_"+currentTime+"/folder_"+currentTime);
        final Host newHost = new SiteDataGen().nextPersisted();

        //Create Folders and SubFolders
        Response responseResource = resource.createFolders(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,foldersToCreate,newHost.getHostname());

        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());

        //Get all the folders and subfolders
        responseResource = resource.loadFolderAndSubFoldersByPath(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,newHost.getIdentifier(),"test_"+currentTime);
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());

        //Check Results
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final FolderView responseFolderView = FolderView.class.cast(responseEntityView.getEntity());
        Assert.assertNotNull("Title is null",responseFolderView.getTitle());
        Assert.assertEquals("Title is not the same as the one requested",responseFolderView.getTitle(),"test_"+currentTime);
        Assert.assertEquals("There is more than one subfolder",1,responseFolderView.getSubFolders().size());
    }

    /**
     * Method to test: loadFolderAndSubFoldersByPath in the FolderResource
     * Given Scenario: Create a few folders/subfolders and get them using the admin as the user, but the hostId sent is not valid
     * ExpectedResult: The endpoint should return IllegalArgumentException that jersey will map to a 400 code
     *
     */
    @Test (expected = IllegalArgumentException.class)
    public void test_loadFolderAndSubFoldersByPath_Admin_InvalidHostId_return400() throws DotDataException, DotSecurityException {

        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test_"+currentTime+"/folder_"+currentTime);
        final Host newHost = new SiteDataGen().nextPersisted();

        //Create Folders and SubFolders
        Response responseResource = resource.createFolders(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,foldersToCreate,newHost.getHostname());

        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());

        //Get all the folders and subfolders
        resource.loadFolderAndSubFoldersByPath(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                UUIDGenerator.uuid(),"test_"+currentTime);
    }

    /**
     * Method to test: loadFolderAndSubFoldersByPath in the FolderResource
     * Given Scenario: Create a few folders/subfolders and get them using the admin as the user, but the path sent is not valid
     * ExpectedResult: The endpoint should return IllegalArgumentException that jersey will map to a 400 code
     *
     */
    @Test (expected = IllegalArgumentException.class)
    public void test_loadFolderAndSubFoldersByPath_Admin_InvalidPath_return400() throws DotDataException, DotSecurityException {

        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test_"+currentTime+"/folder_"+currentTime);
        final Host newHost = new SiteDataGen().nextPersisted();

        //Create Folders and SubFolders
        Response responseResource = resource.createFolders(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,foldersToCreate,newHost.getHostname());

        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());

        //Get all the folders and subfolders
        resource.loadFolderAndSubFoldersByPath(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,
                newHost.getIdentifier(),"folderpathnotexist");
    }

    /**
     * Method to test: loadFolderAndSubFoldersByPath in the FolderResource
     * Given Scenario: Create a few folders/subfolders using the admin user, try to get the created Folders/subFolders
     *                  using a limited user with no permissions over the requested folder.
     * ExpectedResult: The endpoint should return DotSecurityException that jersey will map to a 403 code
     *
     */
    @Test (expected = DotSecurityException.class)
    public void test_loadFolderAndSubFoldersByPath_UserNoPermissionsOverFolder_return403() throws DotDataException, DotSecurityException {

        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test_"+currentTime+"/folder_"+currentTime);
        final Host newHost = new SiteDataGen().nextPersisted();

        //Create Folders and SubFolders
        Response responseResource = resource.createFolders(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,foldersToCreate,newHost.getHostname());

        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());

        //Give Permissions Over the Host
        final Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                newHost.getPermissionId(),
                TestUserUtils.getOrCreatePublisherRole(newHost).getId(),
                (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS), true);
        APILocator.getPermissionAPI().save(permissions, newHost, adminUser, false);

        final User chrisUser = TestUserUtils.getChrisPublisherUser(newHost);
        final String password = "admin";
        chrisUser.setPassword(password);
        APILocator.getUserAPI().save(chrisUser,APILocator.systemUser(),false);

        //Get all the folders and subfolders using the limited user
        resource.loadFolderAndSubFoldersByPath(getHttpRequest(chrisUser.getEmailAddress(),password),response,newHost.getIdentifier(),"test_"+currentTime);
    }

    /**
     * Method to test: loadFolderAndSubFoldersByPath in the FolderResource
     * Given Scenario: Create a few folders/subfolders using the admin user, try to get the created Folders/subFolders
     *                  using a limited user with no permissions over the host where the folder lives.
     * ExpectedResult: The endpoint should return DotSecurityException that jersey will map to a 403 code
     *
     */
    @Test (expected = DotSecurityException.class)
    public void test_loadFolderAndSubFoldersByPath_UserNoPermissionsOverHost_return403() throws DotDataException, DotSecurityException {

        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test_"+currentTime+"/folder_"+currentTime);
        final Host newHost = new SiteDataGen().nextPersisted();

        //Create Folders and SubFolders
        Response responseResource = resource.createFolders(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,foldersToCreate,newHost.getHostname());

        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());

        final User chrisUser = TestUserUtils.getChrisPublisherUser(newHost);
        final String password = "admin";
        chrisUser.setPassword(password);
        APILocator.getUserAPI().save(chrisUser,APILocator.systemUser(),false);

        //Get all the folders and subfolders using the limited user
        responseResource = resource.loadFolderAndSubFoldersByPath(getHttpRequest(chrisUser.getEmailAddress(),password),response,newHost.getIdentifier(),"test_"+currentTime);

    }

    /**
     * Method to test: loadFolderAndSubFoldersByPath in the FolderResource
     * Given Scenario: Create a few folders/subfolders using the admin user, give the limited user permissions
     * over the host and the parent folder (not the subfolder).
     * ExpectedResult: The parent folder only since the user does not have permissions over the subfolder.
     *
     */
    @Test
    public void test_loadFolderAndSubFoldersByPath_UserNoPermissionsOverSubFolder_returnFoldersWithUserPermissions() throws DotDataException, DotSecurityException {

        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test_"+currentTime+"/folder_"+currentTime);
        final Host newHost = new SiteDataGen().nextPersisted();

        //Create Folders and SubFolders
        Response responseResource = resource.createFolders(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,foldersToCreate,newHost.getHostname());

        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());

        //Get all the folders and subfolders using the admin
        responseResource = resource.loadFolderAndSubFoldersByPath(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,newHost.getIdentifier(),"test_"+currentTime);

        //Get The parent folder Id to give permission over it
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        FolderView responseFolderView = FolderView.class.cast(responseEntityView.getEntity());
        final Folder folder = folderAPI.find(responseFolderView.getIdentifier(),adminUser,false);

        //Give Permissions Over the Host
        Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                newHost.getPermissionId(),
                TestUserUtils.getOrCreatePublisherRole(newHost).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, newHost, adminUser, false);
        //Give Permissions Over the Folder
        permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                folder.getPermissionId(),
                TestUserUtils.getOrCreatePublisherRole(newHost).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, folder, adminUser, false);

        final User chrisUser = TestUserUtils.getChrisPublisherUser(newHost);
        final String password = "admin";
        chrisUser.setPassword(password);
        APILocator.getUserAPI().save(chrisUser,APILocator.systemUser(),false);

        //Get all the folders and subfolders using the limited user
        responseResource = resource.loadFolderAndSubFoldersByPath(getHttpRequest(chrisUser.getEmailAddress(),password),response,newHost.getIdentifier(),"test_"+currentTime);

        //Check Results
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        responseFolderView = FolderView.class.cast(responseEntityView.getEntity());
        Assert.assertNotNull("Title is null",responseFolderView.getTitle());
        Assert.assertEquals("Title is not the same as the one requested",responseFolderView.getTitle(),"test_"+currentTime);
        Assert.assertTrue("There is no subfolders since user don't have permissions",responseFolderView.getSubFolders().isEmpty());
    }
}
