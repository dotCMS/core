package com.dotcms.rest.api.v1.folder;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
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
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
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

        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);

        //Give Permissions Over the Host
        final Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                newHost.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, newHost, APILocator.systemUser(), false);

        //Get all the folders and subfolders using the limited user
        resource.loadFolderAndSubFoldersByPath(getHttpRequest(limitedUser.getEmailAddress(),password),response,newHost.getIdentifier(),"test_"+currentTime);
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

        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);

        //Get all the folders and subfolders using the limited user
        responseResource = resource.loadFolderAndSubFoldersByPath(getHttpRequest(limitedUser.getEmailAddress(),password),response,newHost.getIdentifier(),"test_"+currentTime);

    }

    // ============================================================
    // findSubFoldersByPath (POST /api/v1/folder/byPath) tests
    // ============================================================

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Create 25 subfolders under a parent folder; call byPath with no pagination params
     * ExpectedResult: All 25 subfolders (plus the parent) are returned — currently FAILS because the
     *                 hardcoded limit of 20 truncates the subfolder stream to 20
     */
    @Test
    public void test_findSubFoldersByPath_moreThan20Folders_returnsAllFolders() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent").nextPersisted();
        for (int i = 0; i < 25; i++) {
            new FolderDataGen().parent(parent).name(String.format("subfolder%02d", i)).nextPersisted();
        }

        final String path = String.format("//%s/%s/", site.getHostname(), parent.getName());
        final Response res = resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), 0, 40);

        Assert.assertEquals(Status.OK.getStatusCode(), res.getStatus());
        final ResponseEntityView<?> entity = ResponseEntityView.class.cast(res.getEntity());
        final List<FolderSearchResultView> results = (List<FolderSearchResultView>) entity.getEntity();
        // 1 parent + 25 subfolders = 26
        Assert.assertEquals("Expected 26 results (parent + 25 subfolders) but the 20-item cap truncated them", 26, results.size());
    }

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Create 45 subfolders; call with no pagination params (default limit = 40)
     * ExpectedResult: 40 total results — currently FAILS because hardcoded limit is 20
     */
    @Test
    public void test_findSubFoldersByPath_defaultLimit40_returnsUpTo40() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent").nextPersisted();
        for (int i = 0; i < 45; i++) {
            new FolderDataGen().parent(parent).name(String.format("subfolder%02d", i)).nextPersisted();
        }

        final String path = String.format("//%s/%s/", site.getHostname(), parent.getName());
        final Response res = resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), 0, 40);

        Assert.assertEquals(Status.OK.getStatusCode(), res.getStatus());
        final ResponseEntityView<?> entity = ResponseEntityView.class.cast(res.getEntity());
        final List<FolderSearchResultView> results = (List<FolderSearchResultView>) entity.getEntity();
        Assert.assertEquals("Expected 40 results (default limit) but got wrong count", 40, results.size());
    }

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Create 30 subfolders; call with limit=10
     * ExpectedResult: 10 results (the parent + 9 subfolders)
     */
    @Test
    public void test_findSubFoldersByPath_withCustomLimit_returnsLimitedResults() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent").nextPersisted();
        for (int i = 0; i < 30; i++) {
            new FolderDataGen().parent(parent).name(String.format("subfolder%02d", i)).nextPersisted();
        }

        final String path = String.format("//%s/%s/", site.getHostname(), parent.getName());
        final Response res = resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), 0, 10);

        Assert.assertEquals(Status.OK.getStatusCode(), res.getStatus());
        final ResponseEntityView<?> entity = ResponseEntityView.class.cast(res.getEntity());
        final List<FolderSearchResultView> results = (List<FolderSearchResultView>) entity.getEntity();
        Assert.assertEquals("Expected 10 results with limit=10", 10, results.size());
    }

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Create 30 named subfolders; call with offset=10, limit=10
     * ExpectedResult: 10 results that do NOT include the first 10 items
     */
    @Test
    public void test_findSubFoldersByPath_withOffset_returnsCorrectPage() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent").nextPersisted();
        for (int i = 0; i < 30; i++) {
            new FolderDataGen().parent(parent).name(String.format("subfolder%02d", i)).nextPersisted();
        }

        final String path = String.format("//%s/%s/", site.getHostname(), parent.getName());

        // page 1: first 10
        final Response page1Res = resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), 0, 10);
        final List<FolderSearchResultView> page1 = (List<FolderSearchResultView>)
                ResponseEntityView.class.cast(page1Res.getEntity()).getEntity();

        // page 2: next 10
        final Response page2Res = resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), 10, 10);
        final List<FolderSearchResultView> page2 = (List<FolderSearchResultView>)
                ResponseEntityView.class.cast(page2Res.getEntity()).getEntity();

        Assert.assertEquals(10, page1.size());
        Assert.assertEquals(10, page2.size());

        // No overlap between pages
        final List<String> page1Paths = page1.stream().map(FolderSearchResultView::path).toList();
        for (final FolderSearchResultView item : page2) {
            Assert.assertFalse("Page 2 should not contain items from page 1", page1Paths.contains(item.path()));
        }
    }

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Call with an offset beyond the total number of results
     * ExpectedResult: Empty list
     */
    @Test
    public void test_findSubFoldersByPath_offsetBeyondTotal_returnsEmpty() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent").nextPersisted();
        for (int i = 0; i < 5; i++) {
            new FolderDataGen().parent(parent).name(String.format("subfolder%02d", i)).nextPersisted();
        }

        final String path = String.format("//%s/%s/", site.getHostname(), parent.getName());
        final Response res = resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), 100, 10);

        Assert.assertEquals(Status.OK.getStatusCode(), res.getStatus());
        final ResponseEntityView<?> entity = ResponseEntityView.class.cast(res.getEntity());
        final List<FolderSearchResultView> results = (List<FolderSearchResultView>) entity.getEntity();
        Assert.assertTrue("Expected empty results when offset exceeds total", results.isEmpty());
    }

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Create 25 subfolders and call with limit=-1 (no limit)
     * ExpectedResult: All 26 results returned (parent + all 25 subfolders)
     */
    @Test
    public void test_findSubFoldersByPath_limitMinusOne_returnsAll() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent").nextPersisted();
        for (int i = 0; i < 50; i++) {
            new FolderDataGen().parent(parent).name(String.format("subfolder%02d", i)).nextPersisted();
        }

        final String path = String.format("//%s/%s/", site.getHostname(), parent.getName());
        final Response res = resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), 0, -1);

        Assert.assertEquals(Status.OK.getStatusCode(), res.getStatus());
        final ResponseEntityView<?> entity = ResponseEntityView.class.cast(res.getEntity());
        final List<FolderSearchResultView> results = (List<FolderSearchResultView>) entity.getEntity();
        // 1 parent + 50 subfolders = 51
        Assert.assertEquals("limit=-1 should return all folders without cap", 51, results.size());
    }

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Call with a negative offset
     * ExpectedResult: 400 Bad Request (BadRequestException)
     */
    @Test(expected = com.dotcms.rest.exception.BadRequestException.class)
    public void test_findSubFoldersByPath_negativeOffset_throwsBadRequest() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent").nextPersisted();
        final String path = String.format("//%s/%s/", site.getHostname(), parent.getName());
        resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), -1, 40);
    }

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Call with a negative limit other than -1
     * ExpectedResult: 400 Bad Request (BadRequestException)
     */
    @Test(expected = com.dotcms.rest.exception.BadRequestException.class)
    public void test_findSubFoldersByPath_negativeLimitNotMinusOne_throwsBadRequest() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent").nextPersisted();
        final String path = String.format("//%s/%s/", site.getHostname(), parent.getName());
        resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), 0, -2);
    }

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Call with limit=0
     * ExpectedResult: 400 Bad Request (BadRequestException) — zero limit is meaningless
     */
    @Test(expected = com.dotcms.rest.exception.BadRequestException.class)
    public void test_findSubFoldersByPath_limitZero_throwsBadRequest() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent").nextPersisted();
        final String path = String.format("//%s/%s/", site.getHostname(), parent.getName());
        resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), 0, 0);
    }

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Call with extreme offset+limit values that would overflow int arithmetic
     * ExpectedResult: Returns empty list (offset beyond total) — does NOT throw 500
     */
    @Test
    public void test_findSubFoldersByPath_overflowGuard_noServerError() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent").nextPersisted();
        for (int i = 0; i < 3; i++) {
            new FolderDataGen().parent(parent).name(String.format("subfolder%02d", i)).nextPersisted();
        }

        final String path = String.format("//%s/%s/", site.getHostname(), parent.getName());
        final Response res = resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), Integer.MAX_VALUE - 5, 100);

        Assert.assertEquals(Status.OK.getStatusCode(), res.getStatus());
        final ResponseEntityView<?> entity = ResponseEntityView.class.cast(res.getEntity());
        final List<FolderSearchResultView> results = (List<FolderSearchResultView>) entity.getEntity();
        Assert.assertTrue("Overflow guard: should return empty, not throw", results.isEmpty());
    }

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Call without providing a path
     * ExpectedResult: 400 Bad Request (BadRequestException)
     */
    @Test(expected = com.dotcms.rest.exception.BadRequestException.class)
    public void test_findSubFoldersByPath_emptyPath_throwsBadRequest() throws DotDataException, DotSecurityException {
        resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(null), 0, 40);
    }

    /**
     * Method to test: findSubFoldersByPath in the FolderResource
     * Given Scenario: Call with a valid path that has fewer than 40 subfolders (e.g. 5)
     * ExpectedResult: All 5 subfolders returned (backward compat — no params needed to get all results)
     */
    @Test
    public void test_findSubFoldersByPath_fewFolders_returnsAll() throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(site).name("parent").nextPersisted();
        for (int i = 0; i < 5; i++) {
            new FolderDataGen().parent(parent).name(String.format("subfolder%02d", i)).nextPersisted();
        }

        final String path = String.format("//%s/%s/", site.getHostname(), parent.getName());
        final Response res = resource.findSubFoldersByPath(
                getHttpRequest(adminUser.getEmailAddress(), "admin"), response,
                new SearchByPathForm(path), 0, 40);

        Assert.assertEquals(Status.OK.getStatusCode(), res.getStatus());
        final ResponseEntityView<?> entity = ResponseEntityView.class.cast(res.getEntity());
        final List<FolderSearchResultView> results = (List<FolderSearchResultView>) entity.getEntity();
        // 1 parent + 5 subfolders = 6
        Assert.assertEquals(6, results.size());
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

        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);

        //Give Permissions Over the Host
        Permission permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                newHost.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, newHost, APILocator.systemUser(), false);

        //Give Permissions Over the Folder
        permissions = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                folder.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permissions, folder, APILocator.systemUser(), false);

        //Get all the folders and subfolders using the limited user
        responseResource = resource.loadFolderAndSubFoldersByPath(getHttpRequest(limitedUser.getEmailAddress(),password),response,newHost.getIdentifier(),"test_"+currentTime);

        //Check Results
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        responseFolderView = FolderView.class.cast(responseEntityView.getEntity());
        Assert.assertNotNull("Title is null",responseFolderView.getTitle());
        Assert.assertEquals("Title is not the same as the one requested",responseFolderView.getTitle(),"test_"+currentTime);
        Assert.assertTrue("There is no subfolders since user don't have permissions",responseFolderView.getSubFolders().isEmpty());
    }
}
