package com.dotcms.rest.api.v1.theme;


import static com.dotmarketing.business.ThemeAPI.THEME_PNG;
import static com.dotmarketing.business.ThemeAPI.THEME_THUMBNAIL_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.ThemeDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Theme;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.JsonNode;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.internal.util.Base64;
import org.junit.BeforeClass;
import org.junit.Test;

public class ThemeResourceIntegrationTest {

    private static ContentletAPI contentletAPI;
    private static HostAPI hostAPI;
    private static ThemeResource resource;
    static HttpServletResponse response;
    private static UserAPI userAPI;
    private static FolderAPI folderAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        resource = new ThemeResource();
        response = new MockHttpResponse();
        user = TestUserUtils.getAdminUser();

        contentletAPI = APILocator.getContentletAPI();

        hostAPI   = APILocator.getHostAPI();
        userAPI   = APILocator.getUserAPI();
        folderAPI = APILocator.getFolderAPI();
    }

    /**
     * Method to test: findThemes
     * Given Scenario: Find all the themes of a specific host, sending the host id as a filter.
     * ExpectedResult: The endpoint should return 200, created theme should be returned
     *
     */
    @Test
    public void test_FindThemes_WhenHostIdIsSent_ReturnsItsThemes()
            throws DotDataException, DotSecurityException {
        final Host newHost = new SiteDataGen().nextPersisted();
        final Contentlet themeVTL = new ThemeDataGen().site(newHost).nextPersisted();
        final Identifier id = APILocator.getIdentifierAPI().find(themeVTL.getIdentifier());
        final Folder themeFolder = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(),id.getHostId(),user,false);

        final Response responseResource = resource.findThemes(getHttpRequest(null),  response, newHost.getIdentifier(), 0, -1,
                OrderDirection.ASC.name(), null);

        assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final PaginatedArrayList themeList = PaginatedArrayList.class.cast(responseEntityView.getEntity());
        assertEquals(1,themeList.size());
        assertEquals(themeFolder.getIdentifier(),HashMap.class.cast(themeList.get(0)).get("identifier"));
    }

    /**
     * Method to test: findThemes
     * Given Scenario: Tries to find the themes without sending the hostId (required field).
     * ExpectedResult: The endpoint should return BadRequest
     *
     */
    @Test(expected = BadRequestException.class)
    public void test_FindThemes_WhenHostIdIsNotSent_ReturnsBadRequest() {
        resource.findThemes(getHttpRequest(null),  response, "", 0, -1,
                OrderDirection.ASC.name(), null);
    }

    /**
     * Method to test: findThemes
     * Given Scenario: Tries to find the themes but the hostId provided does not belong to any Host.
     * ExpectedResult: The endpoint should return DotDataException 404
     *
     */
    @Test(expected = DotDataException.class)
    public void test_FindThemes_WhenInvalidHostId_ReturnsDotDataException() {
        resource.findThemes(getHttpRequest(null),  response, "Id-Not-Exist", 0, -1,
                OrderDirection.ASC.name(), null);
    }

    /**
     * Method to test: findThemes
     * Given Scenario: Find all the themes of a specific host, sending the host id as a filter but as a limited user.
     * ExpectedResult: DotSecurityException
     *
     */
    @Test(expected = DotSecurityException.class)
    public void test_FindThemes_LimitedUserWithoutPermissions_DotSecurityException()
            throws DotDataException, DotSecurityException {
        final Host newHost = new SiteDataGen().nextPersisted();

        //Create the limited user
        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String password = "admin";
        limitedUser.setPassword(password);
        APILocator.getUserAPI().save(limitedUser,APILocator.systemUser(),false);

        resource.findThemes(getHttpRequest(limitedUser.getEmailAddress()),  response, newHost.getIdentifier(), 0, -1,
                OrderDirection.ASC.name(), null);

    }

    /**
     * Method to test: findThemes
     * Given Scenario: Find all the themes of a specific host, sending the host id as a filter and perPage it's 1.
     * ExpectedResult: Only one theme but the Total Entries should be 2.
     *
     */
    @Test
    public void test_FindThemes_WhenHostIdIsSentAndPerPageEqualsOne_ReturnsOneTheme()
            throws Throwable {
        final Host newHost = new SiteDataGen().nextPersisted();
        final Contentlet themeOneVTL = new ThemeDataGen().site(newHost).nextPersisted();
        Identifier id = APILocator.getIdentifierAPI().find(themeOneVTL.getIdentifier());
        final Folder themeOneFolder = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(),id.getHostId(),user,false);
        final Contentlet themeTwoVTL = new ThemeDataGen().site(newHost).nextPersisted();
        id = APILocator.getIdentifierAPI().find(themeTwoVTL.getIdentifier());
        final Folder themeTwoFolder = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(),id.getHostId(),user,false);

        final Response responseResource = resource.findThemes(getHttpRequest(null),  response, newHost.getIdentifier(), 0, 1,
                OrderDirection.ASC.name(), null);

        assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        final PaginatedArrayList themeList = PaginatedArrayList.class.cast(responseEntityView.getEntity());
        assertEquals(1,themeList.size());
        assertEquals(themeOneFolder.getIdentifier(),HashMap.class.cast(themeList.get(0)).get("identifier"));
        assertEquals(2,Integer.parseInt(responseResource.getHeaderString("X-Pagination-Total-Entries")));
    }

    /**
     * Method to test: findThemes
     * Given Scenario: Find all the themes of a specific host and the theme name (folder name) matches the searchParam sent
     * ExpectedResult: The endpoint should return 200, system theme and created theme should be returned
     *
     */
    @Test
    public void test_FindThemes_WhenHostIdAndSearchParamsAreSent_ReturnsItsThemes()
            throws DotDataException, DotSecurityException {
        final Host newHost = new SiteDataGen().nextPersisted();
        final String themeName = "themeTestParam" + System.currentTimeMillis();
        final Contentlet themeOneVTL = new ThemeDataGen().name(themeName).site(newHost).nextPersisted();
        Identifier id = APILocator.getIdentifierAPI().find(themeOneVTL.getIdentifier());
        final Folder themeOneFolder = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(),id.getHostId(),user,false);
        final Contentlet themeTwoVTL = new ThemeDataGen().site(newHost).nextPersisted();
        id = APILocator.getIdentifierAPI().find(themeTwoVTL.getIdentifier());
        final Folder themeTwoFolder = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(),id.getHostId(),user,false);

        Response responseResource = resource.findThemes(getHttpRequest(null),  response, newHost.getIdentifier(), 0, -1,
                OrderDirection.ASC.name(), null);

        assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());
        ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        PaginatedArrayList themeList = PaginatedArrayList.class.cast(responseEntityView.getEntity());
        assertEquals(2,themeList.size());
        assertEquals(themeTwoFolder.getIdentifier(),HashMap.class.cast(themeList.get(0)).get("identifier"));
        assertEquals(themeOneFolder.getIdentifier(),HashMap.class.cast(themeList.get(1)).get("identifier"));

        responseResource = resource.findThemes(getHttpRequest(null),  response, newHost.getIdentifier(), 0, -1,
                OrderDirection.ASC.name(), themeName);

        assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());
        responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        themeList = PaginatedArrayList.class.cast(responseEntityView.getEntity());
        assertEquals(1,themeList.size());
        assertEquals(themeOneFolder.getIdentifier(),HashMap.class.cast(themeList.get(0)).get("identifier"));
    }
//
//
//    @Test
//    public void test_FindThemeById() throws Throwable {
//        final Folder folderExpected = folderAPI
//                .findFolderByPath("/application/themes/quest", host, user, false);
//        final ThemeResource resource = new ThemeResource();
//        final Response response = resource.findThemeById(getHttpRequest(),  new EmptyHttpResponse(), folderExpected.getInode());
//        assertEquals(Status.OK.getStatusCode(), response.getStatus());
//        final HashMap folder =  (HashMap) ((ResponseEntityView) response.getEntity()).getEntity();
//
//        Map<String, Object> mapExpected = folderExpected.getMap();
//
//        //Considering THEME_THUMBNAIL_KEY
//        assertEquals(mapExpected.size(), folder.size() - 1);
//
//        mapExpected.entrySet().stream().filter(entry -> entry.getKey().equals(THEME_THUMBNAIL_KEY)).forEach(expectedEntry -> assertEquals(expectedEntry.getValue(),
//                folder.get(expectedEntry.getKey())));
//    }
//
//    @Test
//    public void test_FindThemeByIdWithPublishedThemePNG_ReturnsThemeThumbnail()
//            throws Throwable {
//        Contentlet thumbnail;
//        Folder destinationFolder = null;
//
//        final String folderName = "/PublishedThemePNGFolder"+System.currentTimeMillis();
//
//        final File file = File.createTempFile(THEME_PNG.split("\\.")[0], ".png");
//        FileUtil.write(file, "Theme Thumbnail");
//
//        try{
//            destinationFolder = folderAPI
//                    .createFolders(folderName, host, user, false);
//            //Creating theme.png
//            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(destinationFolder,file);
//            fileAssetDataGen.setProperty("title", THEME_PNG);
//            fileAssetDataGen.setProperty("fileName", THEME_PNG);
//            fileAssetDataGen.setProperty("__DOTNAME__", THEME_PNG);
//            fileAssetDataGen.host(host);
//
//            thumbnail = fileAssetDataGen.nextPersisted();
//
//            //Publishing theme.png
//            thumbnail.setIndexPolicy(IndexPolicy.FORCE);
//            contentletAPI.publish(thumbnail, user, false);
//
//            final ThemeResource resource = new ThemeResource();
//            final Response response = resource.findThemeById(getHttpRequest(),  new EmptyHttpResponse(), destinationFolder.getInode());
//            assertEquals(Status.OK.getStatusCode(), response.getStatus());
//
//            Map entity = (Map) ((ResponseEntityView) response.getEntity()).getEntity();
//            assertNotNull(entity.get(THEME_THUMBNAIL_KEY));
//            assertEquals(thumbnail.getIdentifier(), entity.get(THEME_THUMBNAIL_KEY));
//
//        } finally {
//            if (destinationFolder != null && destinationFolder.getInode() != null) {
//                folderAPI.delete(destinationFolder, user, false);
//            }
//        }
//    }
//
//    @Test
//    public void test_FindThemeByIdWithArchivedThemePNG_MustNotReturnThemeThumbnail()
//            throws Throwable {
//
//        Contentlet thumbnail;
//        Folder destinationFolder = null;
//        final String folderName = "/ArchivedThemePNGFolder"+System.currentTimeMillis();
//
//        try{
//
//            destinationFolder = folderAPI
//                    .createFolders(folderName, host, user, false);
//
//            final File file = File.createTempFile(THEME_PNG.split("\\.")[0], ".png");
//            FileUtil.write(file, "Theme Thumbnail");
//
//
//            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(destinationFolder,file);
//            fileAssetDataGen.setProperty("title", THEME_PNG);
//            fileAssetDataGen.setProperty("fileName", THEME_PNG);
//            fileAssetDataGen.setProperty("__DOTNAME__", THEME_PNG);
//
//            thumbnail = fileAssetDataGen.nextPersisted();
//            FileAssetDataGen.archive(thumbnail);
//
//            final ThemeResource resource = new ThemeResource();
//            final Response response = resource.findThemeById(getHttpRequest(),  new EmptyHttpResponse(), destinationFolder.getInode());
//            assertEquals(Status.OK.getStatusCode(), response.getStatus());
//
//            Map entity = (Map) ((ResponseEntityView) response.getEntity()).getEntity();
//            assertNull(entity.get(THEME_THUMBNAIL_KEY));
//
//        } finally {
//            if (destinationFolder != null && destinationFolder.getInode() != null) {
//                folderAPI.delete(destinationFolder, user, false);
//            }
//        }
//    }
//
//    @Test
//    public void test_FindThemeByIdWithUnpublishedThemePNG_MustNotReturnThemeThumbnail()
//            throws Throwable {
//
//        Folder destinationFolder = null;
//
//        try{
//            final String folderName = "/UnpublishedThemePNGFolder"+System.currentTimeMillis();
//
//            destinationFolder = folderAPI
//                    .createFolders(folderName, host, user, false);
//
//            final File file = File.createTempFile(THEME_PNG.split("\\.")[0], ".png");
//            FileUtil.write(file, "Theme Thumbnail");
//
//            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(destinationFolder,file);
//            fileAssetDataGen.setProperty("title", THEME_PNG);
//            fileAssetDataGen.setProperty("fileName", THEME_PNG);
//            fileAssetDataGen.setProperty("__DOTNAME__", THEME_PNG);
//
//            fileAssetDataGen.nextPersisted();
//
//            final ThemeResource resource = new ThemeResource();
//            final Response response = resource.findThemeById(getHttpRequest(),  new EmptyHttpResponse(), destinationFolder.getInode());
//            assertEquals(Status.OK.getStatusCode(), response.getStatus());
//            final Map entity = (Map) (((ResponseEntityView) response.getEntity())).getEntity();
//
//            assertNull(entity.get(THEME_THUMBNAIL_KEY));
//
//        } finally {
//            try {
//                if (destinationFolder != null && destinationFolder.getInode() != null) {
//                    folderAPI.delete(destinationFolder, user, false);
//                }
//            }catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Test
//    public void test_FindThemeById_WhenInvalidID_Returns404Error() throws Throwable {
//        final ThemeResource resource = new ThemeResource();
//        final Response response = resource.findThemeById(getHttpRequest(),  new EmptyHttpResponse(), "123456");
//        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
//    }
//

    private HttpServletRequest getHttpRequest(String userEmail) {
        userEmail = UtilMethods.isSet(userEmail) ? userEmail : "admin@dotcms.com";
        final String userEmailAndPassword = userEmail + ":admin";
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + new String(com.liferay.util.Base64.encode(userEmailAndPassword.getBytes())));


        return request;
    }
}
