package com.dotmarketing.webdav;

import static com.dotcms.unittest.TestUtil.upperCaseRandom;
import static org.mockito.Mockito.when;

import com.bradmcevoy.http.FileResource;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DotWebdavHelperTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private final DotWebdavHelper helper = new DotWebdavHelper();

    private static final List<User> createdUsers = new ArrayList<>();

    @AfterClass
    public static void cleanup() {
        createdUsers.forEach(UserDataGen::remove);
        createdUsers.clear();
    }

    private static User newUserWithPassword(final String password) {
        final User user = new UserDataGen().password(password).nextPersisted();
        createdUsers.add(user);
        return user;
    }

    @Test
    public void Test_Get_Folder_Resource_Then_Get_File_Resource() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String folderPath = String.format("http://localhost:8080/webdav/live/1/%s/%s/",host.getName(),parent.getName());
        final Resource folderResource = helper.getResourceFromURL(folderPath);
        Assert.assertNotNull(folderResource);
        Assert.assertTrue(folderResource instanceof FolderResource);
        final String fileResourcePath = String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName());
        final Resource fileResource = helper.getResourceFromURL(fileResourcePath);
        Assert.assertNotNull(fileResource);
        Assert.assertTrue(fileResource instanceof FileResource);
    }


    @Test
    public void Test_Get_Folder_Resource_Then_Get_File_Resource_Shuffled_Casing() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String folderPath = upperCaseRandom(String.format("http://localhost:8080/webdav/live/1/%s/%s/",host.getName(),parent.getName()), 8);

        final Resource folderResource = helper.getResourceFromURL(folderPath);
        Assert.assertNotNull(folderResource);
        Assert.assertTrue(folderResource instanceof FolderResource);
        final String fileResourcePath = upperCaseRandom(String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName()),8);
        final Resource fileResource = helper.getResourceFromURL(fileResourcePath);
        Assert.assertNotNull(fileResource);
        Assert.assertTrue(fileResource instanceof FileResource);
    }

    @Test
    public void Test_Get_Folder_Resource_For_Non_Existing_Path() throws IOException, DotDataException, DotSecurityException {
        String path = "http://localhost:8080/webdav/live/1/demo.dotcms.com/images/black.png";
        final Resource folderResource = helper.getResourceFromURL(path);
        Assert.assertNull(folderResource);
    }

    @Test
    public void Test_Same_Resource_For_Paths_With_Different_Casing() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String path = String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName());
        final String fileResourcePath1 = upperCaseRandom(path,8);
        final String fileResourcePath2 = upperCaseRandom(path,10);
        Assert.assertNotEquals(fileResourcePath1,fileResourcePath2);
        Assert.assertTrue(helper.isSameResourceURL(fileResourcePath1,fileResourcePath2, file.getName()));
    }

    /**
     * Method to test: DotWebdavHelper.setResourceContent
     * <p>
     * Given Scenario: A file whose name carries a listed extension, in upper or lower case, is
     * written through WebDAV.
     * <p>
     * ExpectedResult: The write is refused and no file resource exists at that path afterwards.
     */
    @Test
    public void Test_setResourceContent_listed_extension_any_case_is_rejected() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final var user = APILocator.getUserAPI().getSystemUser();
        try (MockedStatic<HttpManager> mocked = Mockito.mockStatic(HttpManager.class)) {
            final Request mockedRequest = Mockito.mock(Request.class);
            when(mockedRequest.getUserAgentHeader()).thenReturn("Cyberduck");
            mocked.when(HttpManager::request).thenReturn(mockedRequest);
            for (final String name : new String[] {"sample-1.jsp", "sample-2.JSP"}) {
                final var resourceUri = String.format("/webdav/live/1/%s/%s", host.getName(), name);
                try (InputStream content = new ByteArrayInputStream("qa".getBytes())) {
                    Assert.assertThrows(name, IllegalArgumentException.class,
                            () -> helper.setResourceContent(resourceUri, content, "",
                                    null, java.util.Calendar.getInstance().getTime(), user, true));
                }
                Assert.assertNull("no file may exist at " + resourceUri, helper.loadFile(resourceUri, user));
            }
        }
    }

    /**
     * Method to test: DotWebdavHelper.setResourceContent
     * <p>
     * Given Scenario: A file is created with no content, then the same file is updated also with no
     * content.
     * <p>
     * ExpectedResult: The file is created and updated without exceptions.
     *
     * @throws Exception if an error occurs while updating the file with no content.
     */
    @Test
    public void Test_publishing_existing_file_with_empty_file_should_not_fail() throws Exception {

        final SiteDataGen siteDataGen = new SiteDataGen();
        final Host host = siteDataGen.nextPersisted();
        final var user = APILocator.getUserAPI().getSystemUser();

        final var resourceUri = String.format("/webdav/live/1/%s/test-file.vtl", host.getName());

        try (MockedStatic<HttpManager> mocked = Mockito.mockStatic(HttpManager.class)) {

            // Create a mocked request object
            Request mockedRequest = Mockito.mock(Request.class);
            when(mockedRequest.getUserAgentHeader()).thenReturn("Cyberduck");
            mocked.when(HttpManager::request).thenReturn(mockedRequest);

            try (InputStream emptyFileInputStream =
                    new ByteArrayInputStream(new byte[0])) {// Empty file

                // First publish, the contentlet should be created
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);

                // Second publish, the contentlet should be updated without exceptions
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);

                // Third publish, the contentlet should be updated without exceptions
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);
            }
        }
    }

    /**
     * Method to test: {@link DotWebdavHelper#authorizePrincipal(String, String)}
     * <p>
     * Given Scenario: A user authenticates to WebDAV using a valid API token (JWT) as the Basic-auth
     * password instead of the account password.
     * <p>
     * ExpectedResult: Authentication succeeds and the resolved user is returned. API tokens remain a
     * valid WebDAV credential because they are deliberately issued, revocable and network-bound.
     */
    @Test
    public void Test_authorizePrincipal_with_api_token_as_password_is_accepted() throws Exception {
        final User user = newUserWithPassword("aValidPassword-" + System.currentTimeMillis());
        final Date expires = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        final ApiToken apiToken = APILocator.getApiTokenAPI().persistApiToken(
                user.getUserId(), expires, APILocator.systemUser().getUserId(), "127.0.0.1");
        final String apiJwt = APILocator.getApiTokenAPI().getJWT(apiToken, APILocator.systemUser());

        final User authorized = helper.authorizePrincipal(user.getEmailAddress(), apiJwt);

        Assert.assertNotNull("API-token WebDAV auth should resolve a user", authorized);
        Assert.assertEquals(user.getUserId(), authorized.getUserId());
    }

    /**
     * Method to test: {@link DotWebdavHelper#authorizePrincipal(String, String)}
     * <p>
     * Given Scenario: A user presents a valid session (user) token — the kind minted on every login
     * and stored in the remember-me cookie — as the Basic-auth password for WebDAV.
     * <p>
     * ExpectedResult: Authentication is rejected with a {@link DotSecurityException}. Session tokens
     * are issued automatically, are not individually revocable and carry no network restriction, so
     * they must never substitute for a WebDAV credential (security issue #757). Only API tokens are
     * accepted. The token itself resolves as a valid JWT, proving the rejection is by token type and
     * not because the token is malformed.
     */
    @Test
    public void Test_authorizePrincipal_with_session_user_token_as_password_is_rejected() throws Exception {
        final User user = newUserWithPassword("aValidPassword-" + System.currentTimeMillis());
        final String sessionJwt = JsonWebTokenUtils.getInstance().createUserToken(user, 1);

        // Sanity: the session token is a valid, resolvable JWT — it is only rejected for being USER_TOKEN.
        Assert.assertTrue("session token should resolve as a valid JWT",
                APILocator.getApiTokenAPI().fromJwt(sessionJwt, null).isPresent());

        Assert.assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(user.getEmailAddress(), sessionJwt));
    }

    /**
     * Method to test: {@link DotWebdavHelper#authorizePrincipal(String, String)}
     * <p>
     * Given Scenario: A user presents a password that is neither the account password nor a valid
     * JWT.
     * <p>
     * ExpectedResult: Authentication is rejected with a {@link DotSecurityException}.
     */
    @Test
    public void Test_authorizePrincipal_with_invalid_password_is_rejected() throws Exception {
        final User user = newUserWithPassword("aValidPassword-" + System.currentTimeMillis());

        Assert.assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(user.getEmailAddress(), "not-a-password-nor-a-token"));
    }

}
