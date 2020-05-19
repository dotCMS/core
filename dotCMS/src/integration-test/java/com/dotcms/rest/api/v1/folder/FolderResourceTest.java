package com.dotcms.rest.api.v1.folder;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
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
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import java.util.Arrays;
import java.util.HashMap;
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

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        response = new MockHttpResponse();
        resource = new FolderResource();
        folderAPI = APILocator.getFolderAPI();
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
        final User adminUser = TestUserUtils.getAdminUser();
        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test_"+currentTime+"/folder_"+currentTime,"/test2_"+currentTime+"/","test3_"+currentTime);
        final Host newHost = new SiteDataGen().nextPersisted();

        final Response responseResource = resource.createFolders(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,foldersToCreate,newHost.getHostname());

        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());

        //Check Results
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        List<Map<String,Object>> listOfResults = List.class.cast(responseEntityView.getEntity());
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
     * ExpectedResult: The endpoint should return a 403 code and no folders created
     *
     */
    @Test
    public void test_createFolders_UserNoPermissions_return403() throws DotDataException, DotSecurityException {
        final Host newHost = new SiteDataGen().nextPersisted();
        final User chrisUser = TestUserUtils.getChrisPublisherUser(newHost);
        final String password = "admin";
        chrisUser.setPassword(password);
        APILocator.getUserAPI().save(chrisUser,APILocator.systemUser(),false);
        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test"+currentTime+"/folder"+currentTime,"/test2"+currentTime+"/","test3"+currentTime);

        final Response responseResource = resource.createFolders(getHttpRequest(chrisUser.getEmailAddress(),password),response,foldersToCreate,newHost.getHostname());

        //Check that the response is 403, Forbidden
        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(),responseResource.getStatus());

    }

    /**
     * Method to test: createFolders in the FolderResource
     * Given Scenario: Try to create a few folders using the admin user, but the siteName passed does not belong to any site
     * ExpectedResult: The endpoint should return a 400 code and no folders created
     *
     */
    @Test
    public void test_createFolders_siteNameNotExists_return400() throws DotDataException, DotSecurityException {
        final User adminUser = TestUserUtils.getAdminUser();
        final long currentTime = System.currentTimeMillis();
        final List<String> foldersToCreate = Arrays.asList("test"+currentTime+"/folder"+currentTime,"/test2"+currentTime+"/","test3"+currentTime);

        final Response responseResource = resource.createFolders(getHttpRequest(adminUser.getEmailAddress(),"admin"),response,foldersToCreate,"siteNameNotExists");

        //Check that the response is 400, Bad Request
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),responseResource.getStatus());

    }

}
