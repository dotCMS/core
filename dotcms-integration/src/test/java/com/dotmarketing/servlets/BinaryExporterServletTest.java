package com.dotmarketing.servlets;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockServletPathRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpCaptureResponse;
import com.dotcms.mock.response.MockHttpContentTypeResponse;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.mock.response.MockHttpStatusResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import static com.dotmarketing.business.Role.DOTCMS_BACK_END_USER;
import static com.dotmarketing.business.Role.DOTCMS_FRONT_END_USER;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.RandomStringUtils;
import java.util.Base64;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class BinaryExporterServletTest {

    // Temporary binary png file
    private static class TmpBinaryFile implements Closeable {

        private final Path pngFilePath;

        public TmpBinaryFile(final boolean setContent) throws IOException {
            pngFilePath = Files.createTempFile("tmp", ".png");
            if (setContent) {
                Files.write(pngFilePath, ShortyServletAndTitleImageTest.pngPixel);
            }
        }

        public Path getPath() {
            return pngFilePath;
        }

        public File getFile() {
            return pngFilePath.toFile();
        }

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(pngFilePath);
        }
    }

    private static final String BY_ID = "by-identifier";
    private static final String BY_INODE = "by-inode";

    private static final String NO_PERMISSIONS_REQUIRED = "no-permissions-required";
    private static final String PERMISSIONS_REQUIRED = "permissions-required";

    private static final String AUTH_WITH_CREDENTIALS = "auth-with-credentials";
    private static final String AUTH_WITH_TOKEN = "auth-with-token";
    private static final String NO_AUTH = "no-auth";

    private static final String DEFAULT_LANGUAGE = "default-language";
    private static final String NON_DEFAULT_LANGUAGE = "non-default-language";

    private static Host host;
    private static Role role;
    private static User user;
    private static Language nonDefaultLanguage;
    private static String userEmailAndPassword;
    private static ApiToken apiToken;

    // Shared file assets to avoid recreating per test iteration
    private static Folder sharedFolder;
    private static Contentlet sharedFileAsset;         // no special permissions (anonymous accessible)
    private static Contentlet sharedFileAssetWithPerms; // role-based permissions only

    // Shared file asset for requestWebpImage tests
    private static Contentlet sharedWebpFileAsset;

    /**
     * Prepare testing environment
     */
    @BeforeClass
    public static void prepare() throws Exception {

        // Set testing environment
        IntegrationTestInitService.getInstance().init();

        host = new SiteDataGen().nextPersisted();
        role = new RoleDataGen().nextPersisted();

        final String userPassword = RandomStringUtils.randomAlphabetic(10);
        final String userEmail = RandomStringUtils.randomAlphabetic(5) + "@dotcms.com";
        userEmailAndPassword = userEmail + ":" + userPassword;

        user = new UserDataGen()
                .emailAddress(userEmail).password(userPassword)
                .roles(role, APILocator.getRoleAPI().loadRoleByKey(DOTCMS_BACK_END_USER))
                .nextPersisted();

        nonDefaultLanguage = new LanguageDataGen().nextPersisted();

        apiToken = APILocator.getApiTokenAPI().persistApiToken(
           user.getUserId(), Date.from(Instant.now().plus(Duration.ofDays(10))),
           APILocator.systemUser().getUserId() , "127.0.0.1");

        // Create shared folder and file assets for requestBinaryFile tests
        sharedFolder = new FolderDataGen().site(host).nextPersisted();

        final Path tmpSourcePath1 = Files.createTempFile("shared-noperm-", ".png");
        final Path tmpSourcePath2 = Files.createTempFile("shared-perm-", ".png");
        try {
            Files.write(tmpSourcePath1, ShortyServletAndTitleImageTest.pngPixel);
            Files.write(tmpSourcePath2, ShortyServletAndTitleImageTest.pngPixel);

            // File asset without special permissions (anonymous accessible)
            sharedFileAsset = new FileAssetDataGen(
                    sharedFolder, tmpSourcePath1.toFile()).nextPersisted();
            ContentletDataGen.publish(sharedFileAsset);
            assertTrue(APILocator.getContentletAPI().isInodeIndexed(
                    sharedFileAsset.getInode(), true, 1000));

            // File asset with role-based permissions (separate file to avoid name collision)
            sharedFileAssetWithPerms = new FileAssetDataGen(
                    sharedFolder, tmpSourcePath2.toFile()).nextPersisted();
            ContentletDataGen.publish(sharedFileAssetWithPerms);
            assertTrue(APILocator.getContentletAPI().isInodeIndexed(
                    sharedFileAssetWithPerms.getInode(), true, 1000));
            ServletTestUtils.addPermissions(sharedFileAssetWithPerms, role);
        } finally {
            Files.deleteIfExists(tmpSourcePath1);
            Files.deleteIfExists(tmpSourcePath2);
        }

        // Create shared file asset for requestWebpImage tests
        final URL pngResource = BinaryExporterServletTest.class.getClassLoader()
                .getResource("images/issue21652.png");
        if (pngResource != null && !"jar".equals(pngResource.getProtocol())) {
            sharedWebpFileAsset = new FileAssetDataGen(new File(pngResource.getFile()))
                    .host(host)
                    .setPolicy(IndexPolicy.WAIT_FOR)
                    .nextPersisted();
        }

    }

    /**
     * Clean up testing environment
     */
    @AfterClass
    public static void cleanup() {
        User systemUser = APILocator.systemUser();
        if (UtilMethods.isSet(sharedWebpFileAsset) && UtilMethods.isSet(sharedWebpFileAsset.getInode())) {
            FileAssetDataGen.remove(sharedWebpFileAsset);
        }
        if (UtilMethods.isSet(sharedFileAssetWithPerms) && UtilMethods.isSet(sharedFileAssetWithPerms.getInode())) {
            FileAssetDataGen.remove(sharedFileAssetWithPerms);
        }
        if (UtilMethods.isSet(sharedFileAsset) && UtilMethods.isSet(sharedFileAsset.getInode())) {
            FileAssetDataGen.remove(sharedFileAsset);
        }
        if (UtilMethods.isSet(sharedFolder) && UtilMethods.isSet(sharedFolder.getInode())) {
            FolderDataGen.remove(sharedFolder);
        }
        if (UtilMethods.isSet(host) && UtilMethods.isSet(host.getIdentifier())) {
            try {
                host.setIndexPolicy(IndexPolicy.WAIT_FOR);
                APILocator.getHostAPI().unpublish(host, systemUser, false);
                APILocator.getHostAPI().archive(host, systemUser, false);
                APILocator.getHostAPI().delete(host, systemUser, false);
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(BinaryExporterServletTest.class, "Unable to remove Host.", e);
            }
        }
        if (UtilMethods.isSet(role) && UtilMethods.isSet(role.getId())) {
            RoleDataGen.remove(role);
        }
        if (UtilMethods.isSet(user) && UtilMethods.isSet(user.getUserId())) {
            UserDataGen.remove(user);
        }
    }

    /**
     * Data provider for test cases of requestBinaryFile test method
     * @return Test cases data
     */
    @DataProvider
    public static Object[][] testCases() {
        return new Object[][] {
                {BY_ID, NO_PERMISSIONS_REQUIRED, NO_AUTH, DEFAULT_LANGUAGE},
                {BY_ID, NO_PERMISSIONS_REQUIRED, NO_AUTH, NON_DEFAULT_LANGUAGE},
                {BY_ID, NO_PERMISSIONS_REQUIRED, AUTH_WITH_CREDENTIALS, DEFAULT_LANGUAGE},
                {BY_ID, NO_PERMISSIONS_REQUIRED, AUTH_WITH_CREDENTIALS, NON_DEFAULT_LANGUAGE},
                {BY_ID, PERMISSIONS_REQUIRED, NO_AUTH, DEFAULT_LANGUAGE},
                {BY_ID, PERMISSIONS_REQUIRED, NO_AUTH, NON_DEFAULT_LANGUAGE},
                {BY_ID, PERMISSIONS_REQUIRED, AUTH_WITH_CREDENTIALS, DEFAULT_LANGUAGE},
                {BY_ID, PERMISSIONS_REQUIRED, AUTH_WITH_CREDENTIALS, NON_DEFAULT_LANGUAGE},
                {BY_ID, PERMISSIONS_REQUIRED, AUTH_WITH_TOKEN, DEFAULT_LANGUAGE},
                {BY_ID, PERMISSIONS_REQUIRED, AUTH_WITH_TOKEN, NON_DEFAULT_LANGUAGE},
                {BY_INODE, NO_PERMISSIONS_REQUIRED, NO_AUTH, DEFAULT_LANGUAGE},
                {BY_INODE, NO_PERMISSIONS_REQUIRED, NO_AUTH, NON_DEFAULT_LANGUAGE},
                {BY_INODE, NO_PERMISSIONS_REQUIRED, AUTH_WITH_CREDENTIALS, DEFAULT_LANGUAGE},
                {BY_INODE, NO_PERMISSIONS_REQUIRED, AUTH_WITH_CREDENTIALS, NON_DEFAULT_LANGUAGE},
                {BY_INODE, PERMISSIONS_REQUIRED, NO_AUTH, DEFAULT_LANGUAGE},
                {BY_INODE, PERMISSIONS_REQUIRED, NO_AUTH, NON_DEFAULT_LANGUAGE},
                {BY_INODE, PERMISSIONS_REQUIRED, AUTH_WITH_CREDENTIALS, DEFAULT_LANGUAGE},
                {BY_INODE, PERMISSIONS_REQUIRED, AUTH_WITH_CREDENTIALS, NON_DEFAULT_LANGUAGE},
                {BY_INODE, PERMISSIONS_REQUIRED, AUTH_WITH_TOKEN, DEFAULT_LANGUAGE},
                {BY_INODE, PERMISSIONS_REQUIRED, AUTH_WITH_TOKEN, NON_DEFAULT_LANGUAGE}
        };
    }

    /**
     * Method to test: {@link BinaryExporterServlet.doGet(HttpServletRequest, HttpServletResponse)}
     * Given scenario: Request a binary file asset Expected result: Should return the binary file
     * asset content if permissions are granted If permissions are not granted, should return 401
     * Unauthorized
     *
     * @param byIdType       Identifier type (by-identifier or by-inode)
     * @param permissionType Permissions required (no-permissions-required or permissions-required)
     * @param authType       Authorization type (no-auth, auth-with-credentials or auth-with-token)
     * @param languageType   The type of language to use for the test (default-language,
     *                       non-default-language)
     */
    @UseDataProvider("testCases")
    @Test
    public void requestBinaryFile(final String byIdType, final String permissionType,
            final String authType, final String languageType)
            throws DotDataException, DotSecurityException, ServletException, IOException {

        final boolean byIdentifier = byIdType.equals(BY_ID);
        final boolean permissionsRequired = permissionType.equals(PERMISSIONS_REQUIRED);
        final boolean useDefaultLanguage = languageType.equals(DEFAULT_LANGUAGE);

        // Use shared file assets created in @BeforeClass
        final Contentlet fileAsset = permissionsRequired
                ? sharedFileAssetWithPerms : sharedFileAsset;

        try (TmpBinaryFile tmpTargetFile = new TmpBinaryFile(false)) {

            // Build request and response
            final String fileURI = "/contentAsset/raw-data/"
                    + (byIdentifier ? fileAsset.getIdentifier() : fileAsset.getInode())
                    + "/fileAsset/";
            final MockHeaderRequest request = new MockHeaderRequest(mockServletRequest(fileURI));
            if (AUTH_WITH_CREDENTIALS.equals(authType)) {
                request.setHeader("Authorization",
                        "Basic " + Base64.getEncoder().encodeToString(userEmailAndPassword.getBytes()));
            } else if (AUTH_WITH_TOKEN.equals(authType)) {
                request.setHeader("Authorization",
                        "Bearer " + APILocator.getApiTokenAPI().getJWT(apiToken, user));
            }

            if (!useDefaultLanguage) {
                HttpSession sessionOpt = request.getSession(true);
                if (sessionOpt != null) {
                    sessionOpt.setAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE,
                            String.valueOf(nonDefaultLanguage.getId()));
                }
            }

            final HttpServletResponse response = mockServletResponse(tmpTargetFile);

            // Send servlet request
            sendRequest(request, response);

            if (permissionsRequired && NO_AUTH.equals(authType)) {
                // Verify response status
                if (byIdentifier && !useDefaultLanguage) {
                    assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
                } else {
                    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
                    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
                }
            } else {
                // Verify response
                assertEquals(HttpServletResponse.SC_OK, response.getStatus());
                final byte[] responseContent = Files.readAllBytes(tmpTargetFile.getPath());
                assertArrayEquals(ShortyServletAndTitleImageTest.pngPixel, responseContent);
            }

        }

    }

    /**
     * Method to test: {@link BinaryExporterServlet#doGet(HttpServletRequest, HttpServletResponse)}
     * Given scenario: An authenticated front-end user (logged in, non-anonymous) requests a
     * protected file asset they do NOT have READ permission on.
     * Expected result: A clean 403 Forbidden is returned (matching VelocityServlet page behavior),
     * and REDIRECT_AFTER_LOGIN is NOT set, so SAML sites do not enter an infinite redirect loop.
     * See issue #36541.
     */
    @Test
    public void requestBinaryFile_authenticatedUserWithoutPermission_returns403()
            throws DotDataException, DotSecurityException, ServletException, IOException {

        // Front-end user WITHOUT the role that grants READ on sharedFileAssetWithPerms
        final User frontEndUser = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadRoleByKey(DOTCMS_FRONT_END_USER))
                .nextPersisted();

        try (TmpBinaryFile tmpTargetFile = new TmpBinaryFile(false)) {

            final String fileURI = "/contentAsset/raw-data/"
                    + sharedFileAssetWithPerms.getIdentifier() + "/fileAsset/";
            final MockHeaderRequest request = new MockHeaderRequest(mockServletRequest(fileURI));

            // Simulate an authenticated front-end user on the request (as a session login would)
            request.setAttribute(WebKeys.USER, frontEndUser);

            final HttpServletResponse response = mockServletResponse(tmpTargetFile);

            sendRequest(request, response);

            // Authenticated-but-unauthorized user must get a clean 403, not a 401 redirect-to-login
            assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
            assertNull(request.getSession().getAttribute(
                    com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN));
        } finally {
            UserDataGen.remove(frontEndUser);
        }
    }

    @Test
    @Ignore("This test is failing because a difference with m1 mac native library")
    public void requestGifFile()
            throws DotDataException, DotSecurityException, ServletException, IOException {


        File gifFile = getResourceFile("images/issue19338.gif");

        final Contentlet fileContentlet = new FileAssetDataGen(gifFile).host(host)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();

        final String fileURI = "/contentAsset/image/"+fileContentlet.getInode()+"/fileAsset/byInode/true/quality_q/30/resize_w/200";
        final HttpServletRequest request = mockServletRequest(fileURI);
        request.setAttribute(WebKeys.USER, APILocator.systemUser());

        TmpBinaryFile tmpTargetFile = new TmpBinaryFile(false);
        final HttpServletResponse response = mockServletResponse(tmpTargetFile);

        // Send servlet request
        sendRequest(request, response);
        final byte[] responseContent = Files.readAllBytes(tmpTargetFile.getPath());
        final String expectedContent = "RIFF�\u0003\u0000\u0000WEBPVP8 �\u0003\u0000\u0000\u0010\u001D\u0000�\u0001*�\u0000c\u0000?\u0011x�R�'?���\n"
                + "S�\"\tin�\n"
                + "k\u001F\u001D�\n"
                + "���Wh��/\u0003�k�ߏ]����\u0013q��?�~a���G{��?�~�������A��wv*\u0018��\u0013�p�Z����C$\u0010[\u0000\"���w\u000E�\u000BF���\u001A'���O����z�mg+^��G�\u0018����,\u000B���F[��\"ֲ�5>����~\u0005��h��\u0003E�40\u0006�7�{=R��njש3�n\u0004\u001C6�p!�\u05C8�aA]T\u001A�\u0015(�\u000E8@mh\u0004\u0000�G�.�\u0003Yxx�[�\u000E\u00154�\u001E��p�T\u0000\u0000��q��x�?ջt�l;\u0014N2!�\u000F��6eD���^K�g���KB\u0005Da����u(�1�Gr��C�c�\u001C����?��D�C%O��Q\u00192��́�#�d\u00014�����\u000Bd\u001Aӷ�\u0010V^\u0005h�6�d�r����ںo1\u007F��y\tJ1�h˾�o5\u007F)O�2��\u0016�ꀊ�\u0010\f\u0011\u0010�\u001D3Yzk�\u0000#v-�y�\u0018���L\u001B�zF\u0004{��\u0010\u0006���\u001F�=#\u0002:��\u000B�L\u001D~���IE�q����f���}���[�\u0017lP\u0012\u001D�*�\u0002|�a_C��\u0005��!�]�\u0019V{�<WvY�\"3����j�\u001B-)IңZ\u0010\b\u0012F��'\u0001���R�A�|���<�?���{1Ô�\n"
                + "�w���\u0018�B\u0010r�I�P�e����C\u0013�[�#�Y�l\u000E��B�\u0012(�17\",b\"A�-�\u0012�v���\bϢ�r��?05��\u0010=�Bh\u0010�dS�Ы�ҩ+ɀ��at���>4�2��Fp\u0005|��_�\u0007�\u000F���\b�I\u0017\u0015��YI���{ȡMK�H�`�x͜ǝFj\u001E�8�����\n"
                + "��͢'���q\u0003��\u0002�%k��86����\u0010��$��g�V��\u007F�xi�\\�\t��Ÿ�0�����-��\u001C�C\u000F!A;�\u0017��\u0000i��BϦÒ�\b\t�a����,%u�)���vn'�0|�v4��O�//\u0006��Զ��J��#?L1��\u001F�['�q\u001B��\u001C\u001A-�ȡxwY�\u0016 �L=4M�j[�\u0016tU΅j|�a\u001F\u0015��_-�`_\u0005��kv���\u0007Q&�?ӻ(�\u001C\n"
                + "e�\n"
                + "\u0006�@\tĥ5���mx1����/1��.<%��bߞ�p�E;\u0007S�l�<�\u0011�\u000F��\"\u0003��\u007FY�%�%\u0003�\"O��Ǡ�\u0096�\b'�\u000FU~x�S\u0014��b���S�9��v�Xmo�ڢ}��A�\u0000\u0000\u0000\u0000\u0000\u0000";

        assertTrue(equalsIgnoreNewlineStyle(expectedContent, new String(responseContent, StandardCharsets.UTF_8)));

    }

    private File getResourceFile(String s) {

        // If resource is absolute path then use Thread.currentThread().getContextClassLoader()
        // If resource is relative path then use getClass().getClassLoader();

        ClassLoader classloader = getClass().getClassLoader();

        final URL resource = classloader.getResource(s);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + s);
        }

        // ir resource is in a jar file then throw exception
        if (resource.getProtocol().equals("jar")) {
            throw new IllegalArgumentException("Resource is in a jar file: " + s);
        }
        return new File(resource.getFile());
    }

    @DataProvider
    public static Object[][] testCasesWebp() {
        return new Object[][] {
                { "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.3 Safari/605.1.15", "image/webp" },
                { "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1 Safari/605.1.15", "image/jpeg" },
                { "Mozilla/5.0 (X11; CrOS x86_64 13982.82.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.157 Safari/537.36", "image/webp" },
                { "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36 Edg/90.0.818.46", "image/webp" },
                { "Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/36.0  Mobile/15E148 Safari/605.1.15", "image/webp" },

        };
    }

    /**
     * Method to test: BinaryExporterServlet.doGet
     * Given scenario: Given a webp request
     * Expected result: Should resolve as webp on any browser different than Safari version < 14.
     * On Safari version below 14 should resolve as jpg
     */
    @UseDataProvider("testCasesWebp")
    @Test
    public void requestWebpImage(final String userAgent, final String expectedContentType)
            throws DotDataException, DotSecurityException, ServletException, IOException {

        // Use shared file asset created in @BeforeClass
        final Contentlet fileContentlet = sharedWebpFileAsset;

        final String fileURI = "/contentAsset/image/" + fileContentlet.getInode()
                + "/fileAsset/byInode/true/quality_q/75/resize_w/600/quality_q/75/quality_q/75";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("user-agent")).thenReturn(userAgent);
        when(request.getAttribute(WebKeys.USER)).thenReturn(APILocator.systemUser());
        when(request.getRequestURI()).thenReturn(fileURI);
        when(request.getServletPath()).thenReturn("/contentAsset");
        when(Config.CONTEXT.getMimeType(matches(".*\\.webp")))
                .thenReturn("image/webp");

        when(Config.CONTEXT.getMimeType(matches(".*\\.jpg")))
                .thenReturn("image/jpeg");

        final HttpServletResponse response = new MockHttpContentTypeResponse(
                new MockHttpResponse().response());

        // Send servlet request
        final BinaryExporterServlet binaryExporterServlet = new BinaryExporterServlet();
        binaryExporterServlet.init();
        binaryExporterServlet.doGet(request, response);

        assertEquals(expectedContentType, response.getContentType());

    }

    public static boolean equalsIgnoreNewlineStyle(String s1, String s2) {
        return s1 != null && s2 != null && normalizeLineEnds(s1).equals(normalizeLineEnds(s2));
    }

    private static String normalizeLineEnds(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }

    private HttpServletRequest mockServletRequest(final String fileURI) {
        return new MockSessionRequest(new MockServletPathRequest(
                new MockHttpRequestIntegrationTest("localhost", fileURI).request(),
                "/contentAsset"));
    }

    private HttpServletResponse mockServletResponse(final TmpBinaryFile tmpTargetFile) {
        try {
            return new MockHttpStatusResponse(new MockHttpCaptureResponse(
                    mock(HttpServletResponse.class), new FileOutputStream(tmpTargetFile.getFile())));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendRequest(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        final BinaryExporterServlet binaryExporterServlet = new BinaryExporterServlet();
        binaryExporterServlet.init();
        binaryExporterServlet.doGet(request, response);

    }


}
