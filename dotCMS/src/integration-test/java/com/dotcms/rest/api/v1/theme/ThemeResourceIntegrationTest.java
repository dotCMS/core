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
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
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
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;


import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;

public class ThemeResourceIntegrationTest {

    private static ContentletAPI contentletAPI;
    private static HostAPI hostAPI;
    private static UserAPI userAPI;
    private static User user;
    private static Host host;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        contentletAPI = APILocator.getContentletAPI();

        hostAPI = APILocator.getHostAPI();
        userAPI = APILocator.getUserAPI();

        user = userAPI.getSystemUser();
        host = hostAPI.findDefaultHost(user, false);
    }

    @Test
    public void test_FindThemes_WhenHostIdIsSent_ReturnsItsThemes()
            throws Throwable {
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemes(getHttpRequest(), host.getIdentifier(), 0, -1,
                OrderDirection.ASC.name(), null);

        validateResponse(response, -1);
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

        validateResponse(response, 1);
    }


    @Test
    public void test_FindThemes_WhenHostIdAndSearchParamsAreSent_ReturnsAllThemesThatMatches()
            throws Throwable {
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemes(getHttpRequest(), host.getIdentifier(), 0, -1,
                OrderDirection.ASC.name(), "ne");

        validateResponse(response, -1);
    }

    @Test
    public void test_FindThemes_WhenHostIdAndSearchParamsAreSentAndPerPageEquals1_Returns1Theme()
            throws Throwable {
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemes(getHttpRequest(), host.getIdentifier(), 0, 1,
                OrderDirection.ASC.name(), "ne");

        validateResponse(response, 1);
    }

    @Test
    public void test_FindThemeById() throws Throwable {
        final Folder folderExpected = APILocator.getFolderAPI()
                .findFolderByPath("/application/themes/quest", host, user, false);
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemeById(getHttpRequest(), folderExpected.getInode());
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        final HashMap folder =  (HashMap) ((ResponseEntityView) response.getEntity()).getEntity();

        assertEquals(folderExpected.getMap(), folder);
    }

    @Test
    public void test_FindThemeByIdWithPublishedThemePNG_ReturnsThemeThumbnail()
            throws Throwable {
        Contentlet thumbnail = null;
        final Folder folder = APILocator.getFolderAPI()
                .findFolderByPath("/application/themes/quest", host, user, false);

        final File file = File.createTempFile(THEME_PNG.split("\\.")[0], ".png");
        FileUtil.write(file, "Theme Thumbnail");

        try{

            //Creating theme.png
            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder,file);
            fileAssetDataGen.setProperty("title", THEME_PNG);
            fileAssetDataGen.setProperty("fileName", THEME_PNG);
            fileAssetDataGen.setProperty("__DOTNAME__", THEME_PNG);

            thumbnail = fileAssetDataGen.nextPersisted();

            //Publishing theme.png
            contentletAPI.publish(thumbnail, user, false);
            contentletAPI.isInodeIndexed(thumbnail.getInode());

            final ThemeResource resource = new ThemeResource();
            final Response response = resource.findThemeById(getHttpRequest(), folder.getInode());
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            final String responseString = response.getEntity().toString();
            final JsonNode jsonNode = objectMapper.readTree(responseString);

            final List<JsonNode> responseList = CollectionsUtils
                    .asList(jsonNode.get("entity").elements());

            assertNotNull(responseList.get(0).get(THEME_THUMBNAIL_KEY));
            assertEquals(thumbnail.getIdentifier(), responseList.get(0).get(THEME_THUMBNAIL_KEY).textValue());

        } finally {

            if (UtilMethods.isSet(thumbnail)){
                FileAssetDataGen.archive(thumbnail);
                FileAssetDataGen.delete(thumbnail);
            }

        }
    }

    @Test
    public void test_FindThemeByIdWithArchivedThemePNG_MustNotReturnThemeThumbnail()
            throws Throwable {
        Contentlet thumbnail = null;
        final Folder folder = APILocator.getFolderAPI()
                .findFolderByPath("/application/themes/quest", host, user, false);

        final File file = File.createTempFile(THEME_PNG.split("\\.")[0], ".png");
        FileUtil.write(file, "Theme Thumbnail");

        try{
            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder,file);
            fileAssetDataGen.setProperty("title", THEME_PNG);
            fileAssetDataGen.setProperty("fileName", THEME_PNG);
            fileAssetDataGen.setProperty("__DOTNAME__", THEME_PNG);

            thumbnail = fileAssetDataGen.nextPersisted();
            FileAssetDataGen.archive(thumbnail);

            final ThemeResource resource = new ThemeResource();
            final Response response = resource.findThemeById(getHttpRequest(), folder.getInode());
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            final String responseString = response.getEntity().toString();
            final JsonNode jsonNode = objectMapper.readTree(responseString);

            final List<JsonNode> responseList = CollectionsUtils
                    .asList(jsonNode.get("entity").elements());
            assertNull(responseList.get(0).get(THEME_THUMBNAIL_KEY).textValue());

        } finally {

            if (UtilMethods.isSet(thumbnail)){
                if (!thumbnail.isArchived()) {
                    FileAssetDataGen.archive(thumbnail);
                }
                FileAssetDataGen.delete(thumbnail);
            }

        }
    }

    @Test
    public void test_FindThemeByIdWithUnpublishedThemePNG_MustNotReturnThemeThumbnail()
            throws Throwable {
        Contentlet thumbnail = null;
        final Folder folder = APILocator.getFolderAPI()
                .findFolderByPath("/application/themes/quest", host, user, false);

        final File file = File.createTempFile(THEME_PNG.split("\\.")[0], ".png");
        FileUtil.write(file, "Theme Thumbnail");

        try{
            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder,file);
            fileAssetDataGen.setProperty("title", THEME_PNG);
            fileAssetDataGen.setProperty("fileName", THEME_PNG);
            fileAssetDataGen.setProperty("__DOTNAME__", THEME_PNG);

            thumbnail = fileAssetDataGen.nextPersisted();

            final ThemeResource resource = new ThemeResource();
            final Response response = resource.findThemeById(getHttpRequest(), folder.getInode());
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            final String responseString = response.getEntity().toString();
            final JsonNode jsonNode = objectMapper.readTree(responseString);

            final List<JsonNode> responseList = CollectionsUtils
                    .asList(jsonNode.get("entity").elements());
            assertNull(responseList.get(0).get(THEME_THUMBNAIL_KEY).textValue());

        } finally {

            if (UtilMethods.isSet(thumbnail)){
                FileAssetDataGen.archive(thumbnail);
                FileAssetDataGen.delete(thumbnail);
            }

        }
    }

    @Test
    public void test_FindThemeById_WhenInvalidID_Returns404Error() throws Throwable {
        final ThemeResource resource = new ThemeResource();
        final Response response = resource.findThemeById(getHttpRequest(), "123456");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    private void validateResponse(final Response response, final int perPage) throws IOException {
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        final String totalEntries = response.getHeaderString("X-Pagination-Total-Entries");
        final String responseString = response.getEntity().toString();
        final JsonNode jsonNode = objectMapper.readTree(responseString);

        final List<JsonNode> responseList = CollectionsUtils
                .asList(jsonNode.get("entity").elements());

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
