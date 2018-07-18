package com.dotcms.rest.api.v1.theme;


import static com.dotmarketing.business.ThemeAPI.THEME_PNG;
import static com.dotmarketing.business.ThemeAPI.THEME_THUMBNAIL_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonNode;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.repackage.org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UtilMethods;


import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.Map;

public class ThemeResourceIntegrationTest {

    private static ContentletAPI contentletAPI;
    private static HostAPI hostAPI;
    private static UserAPI userAPI;
    private static FolderAPI folderAPI;
    private static User user;
    private static Host host;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        contentletAPI = APILocator.getContentletAPI();

        hostAPI   = APILocator.getHostAPI();
        userAPI   = APILocator.getUserAPI();
        folderAPI = APILocator.getFolderAPI();

        user = userAPI.getSystemUser();
        host = hostAPI.findDefaultHost(user, false);
    }

    @Test
    public void test_FindThemes_WhenHostIdIsSent_ReturnsItsThemes()
            throws Throwable {
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemes(getHttpRequest(), host.getIdentifier(), 0, -1,
                OrderDirection.ASC.name(), null);

        validateFindThemesResponse(response, -1);
    }

    @Test
    public void test_FindThemes_WhenInvalidHostId_Returns404Error()
            throws Throwable {
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemes(getHttpRequest(), "123456", 0, -1,
                OrderDirection.ASC.name(), null);

        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void test_FindThemes_WhenHostIdIsSentAndPerPageEquals1_Returns1Theme()
            throws Throwable {
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemes(getHttpRequest(), host.getIdentifier(), 0, 1,
                OrderDirection.ASC.name(), null);

        validateFindThemesResponse(response, 1);
    }


    @Test
    public void test_FindThemes_WhenHostIdAndSearchParamsAreSent_ReturnsAllThemesThatMatches()
            throws Throwable {
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemes(getHttpRequest(), host.getIdentifier(), 0, -1,
                OrderDirection.ASC.name(), "ne");

        validateFindThemesResponse(response, -1);
    }

    @Test
    public void test_FindThemes_WhenHostIdAndSearchParamsAreSentAndPerPageEquals1_Returns1Theme()
            throws Throwable {
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemes(getHttpRequest(), host.getIdentifier(), 0, 1,
                OrderDirection.ASC.name(), "ne");

        validateFindThemesResponse(response, 1);
    }

    @Test
    public void test_FindThemeById() throws Throwable {
        final Folder folderExpected = folderAPI
                .findFolderByPath("/application/themes/quest", host, user, false);
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemeById(getHttpRequest(), folderExpected.getInode());
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        final HashMap folder =  (HashMap) ((ResponseEntityView) response.getEntity()).getEntity();

        Map<String, Object> mapExpected = folderExpected.getMap();

        //Considering THEME_THUMBNAIL_KEY
        assertEquals(mapExpected.size(), folder.size() - 1);

        mapExpected.entrySet().stream().filter(entry -> entry.getKey().equals(THEME_THUMBNAIL_KEY)).forEach(expectedEntry -> assertEquals(expectedEntry.getValue(),
                folder.get(expectedEntry.getKey())));
    }

    @Test
    public void test_FindThemeByIdWithPublishedThemePNG_ReturnsThemeThumbnail()
            throws Throwable {
        Contentlet thumbnail;
        Folder destinationFolder = null;

        final String folderName = "/PublishedThemePNGFolder"+System.currentTimeMillis();

        final File file = File.createTempFile(THEME_PNG.split("\\.")[0], ".png");
        FileUtil.write(file, "Theme Thumbnail");

        try{
            destinationFolder = folderAPI
                    .createFolders(folderName, host, user, false);
            //Creating theme.png
            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(destinationFolder,file);
            fileAssetDataGen.setProperty("title", THEME_PNG);
            fileAssetDataGen.setProperty("fileName", THEME_PNG);
            fileAssetDataGen.setProperty("__DOTNAME__", THEME_PNG);

            thumbnail = fileAssetDataGen.nextPersisted();

            //Publishing theme.png
            contentletAPI.publish(thumbnail, user, false);
            contentletAPI.isInodeIndexed(thumbnail.getInode());

            DateUtil.sleep(2000L);
            final ThemeResource resource = new ThemeResource();
            final Response response = resource.findThemeById(getHttpRequest(), destinationFolder.getInode());
            assertEquals(Status.OK.getStatusCode(), response.getStatus());

            Map entity = (Map) ((ResponseEntityView) response.getEntity()).getEntity();
            assertNotNull(entity.get(THEME_THUMBNAIL_KEY));
            assertEquals(thumbnail.getIdentifier(), entity.get(THEME_THUMBNAIL_KEY));

        } finally {
            if (destinationFolder != null && destinationFolder.getInode() != null) {
                folderAPI.delete(destinationFolder, user, false);
            }
        }
    }

    @Test
    public void test_FindThemeByIdWithArchivedThemePNG_MustNotReturnThemeThumbnail()
            throws Throwable {

        Contentlet thumbnail;
        Folder destinationFolder = null;
        final String folderName = "/ArchivedThemePNGFolder"+System.currentTimeMillis();

        try{

            destinationFolder = folderAPI
                    .createFolders(folderName, host, user, false);

            final File file = File.createTempFile(THEME_PNG.split("\\.")[0], ".png");
            FileUtil.write(file, "Theme Thumbnail");


            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(destinationFolder,file);
            fileAssetDataGen.setProperty("title", THEME_PNG);
            fileAssetDataGen.setProperty("fileName", THEME_PNG);
            fileAssetDataGen.setProperty("__DOTNAME__", THEME_PNG);

            thumbnail = fileAssetDataGen.nextPersisted();
            FileAssetDataGen.archive(thumbnail);

            final ThemeResource resource = new ThemeResource();
            final Response response = resource.findThemeById(getHttpRequest(), destinationFolder.getInode());
            assertEquals(Status.OK.getStatusCode(), response.getStatus());

            Map entity = (Map) ((ResponseEntityView) response.getEntity()).getEntity();
            assertNull(entity.get(THEME_THUMBNAIL_KEY));

        } finally {
            if (destinationFolder != null && destinationFolder.getInode() != null) {
                folderAPI.delete(destinationFolder, user, false);
            }
        }
    }

    @Test
    public void test_FindThemeByIdWithUnpublishedThemePNG_MustNotReturnThemeThumbnail()
            throws Throwable {

        Folder destinationFolder = null;

        try{
            final String folderName = "/UnpublishedThemePNGFolder"+System.currentTimeMillis();

            destinationFolder = folderAPI
                    .createFolders(folderName, host, user, false);

            final File file = File.createTempFile(THEME_PNG.split("\\.")[0], ".png");
            FileUtil.write(file, "Theme Thumbnail");

            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(destinationFolder,file);
            fileAssetDataGen.setProperty("title", THEME_PNG);
            fileAssetDataGen.setProperty("fileName", THEME_PNG);
            fileAssetDataGen.setProperty("__DOTNAME__", THEME_PNG);

            fileAssetDataGen.nextPersisted();

            final ThemeResource resource = new ThemeResource();
            final Response response = resource.findThemeById(getHttpRequest(), destinationFolder.getInode());
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            final Map entity = (Map) (((ResponseEntityView) response.getEntity())).getEntity();

            assertNull(entity.get(THEME_THUMBNAIL_KEY));

        } finally {
            if (destinationFolder != null && destinationFolder.getInode()!= null){
                folderAPI.delete(destinationFolder, user, false);
            }
        }
    }

    @Test
    public void test_FindThemeById_WhenInvalidID_Returns404Error() throws Throwable {
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemeById(getHttpRequest(), "123456");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    private void validateFindThemesResponse(final Response response, final int perPage) throws IOException {
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        final String totalEntries = response.getHeaderString("X-Pagination-Total-Entries");
        final Collection entities = (Collection) ((ResponseEntityView) response.getEntity()).getEntity();

        final List<JsonNode> responseList = CollectionsUtils
                .asList(entities.iterator());

        assertTrue(UtilMethods.isSet(responseList));

        //verify pagination
        if (perPage > 0) {
            assertEquals(1, responseList.size());
            assertNotNull(totalEntries);
            assertTrue(Integer.parseInt(totalEntries) > 1);
        }
    }

    /**
     * BasicAuth
     */
    private HttpServletRequest getHttpRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        return request;
    }
}
