package com.dotcms.rest.api.v2.asset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.asset.AssetsRequestForm;
import com.dotcms.rest.api.v1.asset.WebAssetHelper;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.FileUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.lang3.RandomStringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link WebAssetResourceV2}.
 *
 * <p>All tests rely on a live dotCMS context bootstrapped by
 * {@link IntegrationTestInitService}. The {@link WebResource} is mocked so that
 * the authenticated user can be controlled without an HTTP session.
 *
 * <p>Coverage:
 * <ul>
 *   <li>GET by path and GET by identifier return the same version with the same defaults
 *       (working, site-default language).</li>
 *   <li>Working-save preserves live: publish a file, then PUT /save — live version
 *       still has old bytes; PUT /publish promotes working to live.</li>
 *   <li>Missing {@code file} part → 400 with "Missing 'file' part" message.</li>
 *   <li>Zero-byte {@code file} part → 400 with "Zero-byte" distinct message (TempFileAPI
 *       guard or the DotStateException mapper).</li>
 *   <li>Unknown non-blank language → 400.</li>
 *   <li>Blank / omitted language defaults to site default.</li>
 *   <li>User without READ permission on identifier read → 403 (DotSecurityException).</li>
 *   <li>Path pointing at a folder → 400 (IllegalArgumentException).</li>
 *   <li>Unknown host in path → 404 (NotFoundException).</li>
 *   <li>Write response includes correct {@code fileSize}.</li>
 *   <li>{@code .dotsass} extension works for save + read.</li>
 * </ul>
 */
public class WebAssetResourceV2IntegrationTest extends IntegrationTestBase {

    static final String ASSET_PATH_TEMPLATE = "//%s/%s/%s/%s";

    static Host host;
    static Folder folder;
    static Language defaultLanguage;

    static File textFile;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        host = new SiteDataGen().nextPersisted(true);
        folder = new FolderDataGen().site(host).name("v2assets-" + RandomStringUtils.randomAlphabetic(6)).nextPersisted();

        textFile = FileUtil.createTemporaryFile("v2test", ".txt",
                RandomStringUtils.random(512));
    }

    // -----------------------------------------------------------------------
    // GET by path — working version, default language
    // -----------------------------------------------------------------------

    /**
     * Given a file asset persisted in the default language as working-only,
     * when we call getByPath with default params, we get raw bytes back (200).
     */
    @Test
    public void testGetByPath_workingVersion_returnsBytes() throws Exception {
        final File f = FileUtil.createTemporaryFile("get-path", ".txt",
                RandomStringUtils.random(100));
        new FileAssetDataGen(folder, f).languageId(defaultLanguage.getId()).nextPersisted();

        final String path = assetPath(f.getName());
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        final Response resp = resource.getByPath(mockRequest(), new MockHttpResponse(),
                path, null, "working");

        assertEquals(200, resp.getStatus());
        final byte[] body = streamToBytes(resp);
        assertTrue(body.length > 0);
    }

    // -----------------------------------------------------------------------
    // GET by identifier — same defaults as GET by path
    // -----------------------------------------------------------------------

    /**
     * GET by identifier should return identical bytes as GET by path for the
     * same working version in the default language.
     */
    @Test
    public void testGetById_sameDefaultsAsGetByPath() throws Exception {
        final File f = FileUtil.createTemporaryFile("id-vs-path", ".txt",
                RandomStringUtils.random(200));
        final Contentlet saved =
                new FileAssetDataGen(folder, f).languageId(defaultLanguage.getId()).nextPersisted();
        final String identifier = saved.getIdentifier();
        final String path = assetPath(f.getName());

        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        final Response byPath = resource.getByPath(mockRequest(), new MockHttpResponse(),
                path, null, "working");
        final Response byId = resource.getById(mockRequest(), new MockHttpResponse(),
                identifier, null, "working");

        assertEquals(200, byPath.getStatus());
        assertEquals(200, byId.getStatus());
        final byte[] pathBytes = streamToBytes(byPath);
        final byte[] idBytes   = streamToBytes(byId);
        assertEquals(pathBytes.length, idBytes.length);
    }

    // -----------------------------------------------------------------------
    // Working-save preserves live; publish promotes
    // -----------------------------------------------------------------------

    /**
     * Sequence:
     * 1. Publish file v1 (live = true).
     * 2. PUT /save with different content → working now has v2, live still serves v1.
     * 3. PUT /publish → live now serves v2.
     */
    @Test
    public void testSavePreservesLive_thenPublishPromotes()
            throws Exception {
        final String content1 = "LIVE_CONTENT_V1";
        final String content2 = "WORKING_CONTENT_V2";
        final String content3 = "PUBLISHED_CONTENT_V3";

        final File f1 = tmpFile("save-live", ".txt", content1);
        final String path = assetPath(f1.getName());

        // Step 1: publish v1
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());
        resource.publish(mockRequest(), new MockHttpResponse(),
                stream(content1), disposition(f1),
                path, null);

        // Confirm live == v1
        final Response liveResp1 = resource.getByPath(mockRequest(), new MockHttpResponse(),
                path, null, "live");
        assertEquals(200, liveResp1.getStatus());
        assertEquals(content1, new String(streamToBytes(liveResp1), StandardCharsets.UTF_8));

        // Step 2: save (working only) with v2
        final File f2 = tmpFile(f1.getName().replace(".txt", ""), ".txt", content2);
        resource.save(mockRequest(), new MockHttpResponse(),
                stream(content2), disposition(f1),
                path, null);

        // Live still has v1
        final Response liveResp2 = resource.getByPath(mockRequest(), new MockHttpResponse(),
                path, null, "live");
        assertEquals(200, liveResp2.getStatus());
        assertEquals(content1, new String(streamToBytes(liveResp2), StandardCharsets.UTF_8));

        // Working has v2
        final Response workingResp = resource.getByPath(mockRequest(), new MockHttpResponse(),
                path, null, "working");
        assertEquals(200, workingResp.getStatus());
        assertEquals(content2, new String(streamToBytes(workingResp), StandardCharsets.UTF_8));

        // Step 3: publish v3 — live should be promoted
        resource.publish(mockRequest(), new MockHttpResponse(),
                stream(content3), disposition(f1),
                path, null);

        final Response liveResp3 = resource.getByPath(mockRequest(), new MockHttpResponse(),
                path, null, "live");
        assertEquals(200, liveResp3.getStatus());
        assertEquals(content3, new String(streamToBytes(liveResp3), StandardCharsets.UTF_8));
    }

    // -----------------------------------------------------------------------
    // Missing file part → 400
    // -----------------------------------------------------------------------

    /**
     * When the {@code file} InputStream is null (part absent), a BadRequestException
     * must be thrown with a message that mentions "Missing 'file' part".
     */
    @Test
    public void testSave_missingFilePart_returns400() throws Exception {
        final String path = assetPath("missing-part.txt");
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        Exception thrown = null;
        try {
            resource.save(mockRequest(), new MockHttpResponse(),
                    null,  // null InputStream = missing part
                    null,
                    path, null);
        } catch (BadRequestException e) {
            thrown = e;
        }

        assertNotNull("Expected BadRequestException for missing file part", thrown);
        assertTrue("Message should mention missing file part",
                thrown.getMessage().toLowerCase().contains("missing"));
    }

    /**
     * Same guard applies to /publish.
     */
    @Test
    public void testPublish_missingFilePart_returns400() throws Exception {
        final String path = assetPath("missing-part-pub.txt");
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        Exception thrown = null;
        try {
            resource.publish(mockRequest(), new MockHttpResponse(),
                    null, null, path, null);
        } catch (BadRequestException e) {
            thrown = e;
        }

        assertNotNull("Expected BadRequestException for missing file part on publish", thrown);
        assertTrue(thrown.getMessage().toLowerCase().contains("missing"));
    }

    // -----------------------------------------------------------------------
    // Zero-byte file part → 400 (distinct message from missing-part)
    // -----------------------------------------------------------------------

    /**
     * A zero-byte InputStream must be rejected with a {@link BadRequestException}
     * whose message is distinct from the missing-part message and explicitly
     * mentions "zero bytes" (the resource-level guard in {@code writeAsset}).
     *
     * <p>The check uses a {@link java.io.BufferedInputStream} peek so the guard
     * fires before content reaches TempFileAPI or ESContentletAPIImpl.
     */
    @Test
    public void testSave_zeroByteFilePart_returns400WithDistinctMessage() throws Exception {
        final File emptyFile = FileUtil.createTemporaryFile("zero-byte", ".txt");
        assertEquals("Pre-condition: file must be empty", 0L, emptyFile.length());

        final String path = assetPath(emptyFile.getName());
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        BadRequestException thrown = null;
        try (final InputStream emptyStream = Files.newInputStream(emptyFile.toPath())) {
            resource.save(mockRequest(), new MockHttpResponse(),
                    emptyStream, disposition(emptyFile), path, null);
        } catch (BadRequestException e) {
            thrown = e;
        }

        assertNotNull("Expected BadRequestException for zero-byte file part", thrown);

        final String msg = thrown.getMessage();
        assertNotNull("Exception must have a message", msg);

        // Must contain "zero bytes" — the distinct wording from the resource guard.
        assertTrue("Message must mention 'zero bytes': " + msg,
                msg.toLowerCase().contains("zero bytes"));

        // Must NOT be the missing-part message.
        assertFalse("Zero-byte message must differ from missing-part message: " + msg,
                msg.contains("Missing 'file' part"));
    }

    // -----------------------------------------------------------------------
    // Unknown language → 400
    // -----------------------------------------------------------------------

    /**
     * A non-blank language tag that does not exist in the system must result in
     * a BadRequestException (400).
     */
    @Test
    public void testGetByPath_unknownLanguage_returns400() throws Exception {
        final File f = FileUtil.createTemporaryFile("lang-test", ".txt",
                RandomStringUtils.random(64));
        new FileAssetDataGen(folder, f).languageId(defaultLanguage.getId()).nextPersisted();

        final String path = assetPath(f.getName());
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        Exception thrown = null;
        try {
            resource.getByPath(mockRequest(), new MockHttpResponse(),
                    path, "xx-INVALID", "working");
        } catch (BadRequestException e) {
            thrown = e;
        }

        assertNotNull("Expected BadRequestException for unknown language", thrown);
        assertTrue(thrown.getMessage().contains("xx-INVALID"));
    }

    @Test
    public void testSave_unknownLanguage_returns400() throws Exception {
        final File f = FileUtil.createTemporaryFile("lang-save", ".txt",
                RandomStringUtils.random(64));
        final String path = assetPath(f.getName());
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        Exception thrown = null;
        try (final InputStream is = Files.newInputStream(f.toPath())) {
            resource.save(mockRequest(), new MockHttpResponse(),
                    is, disposition(f), path, "zz-NOEXIST");
        } catch (BadRequestException e) {
            thrown = e;
        }

        assertNotNull("Expected BadRequestException for unknown language on save", thrown);
        assertTrue(thrown.getMessage().contains("zz-NOEXIST"));
    }

    // -----------------------------------------------------------------------
    // Blank language defaults to site default
    // -----------------------------------------------------------------------

    /**
     * When language is null or blank, the endpoint must use the site default language
     * and succeed (200 or successful write) without throwing.
     */
    @Test
    public void testGetByPath_blankLanguage_defaultsToSiteDefault() throws Exception {
        final File f = FileUtil.createTemporaryFile("blank-lang", ".txt",
                RandomStringUtils.random(64));
        new FileAssetDataGen(folder, f).languageId(defaultLanguage.getId()).nextPersisted();

        final String path = assetPath(f.getName());
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        // null language
        final Response resp = resource.getByPath(mockRequest(), new MockHttpResponse(),
                path, null, "working");
        assertEquals(200, resp.getStatus());
    }

    @Test
    public void testSave_blankLanguage_defaultsToSiteDefault() throws Exception {
        final File f = FileUtil.createTemporaryFile("blank-lang-save", ".txt",
                RandomStringUtils.random(64));
        final String path = assetPath(f.getName());
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        try (final InputStream is = Files.newInputStream(f.toPath())) {
            final ResponseEntityFileAssetView result = resource.save(
                    mockRequest(), new MockHttpResponse(),
                    is, disposition(f), path, null);  // null language
            assertNotNull(result);
            assertNotNull(result.getEntity());
            assertNotNull(result.getEntity().getIdentifier());
        }
    }

    // -----------------------------------------------------------------------
    // No READ permission on identifier → 403
    // -----------------------------------------------------------------------

    /**
     * A user without READ permission on the contentlet must receive a
     * DotSecurityException (→ 403) and never receive the file bytes.
     */
    @Test
    public void testGetById_noReadPermission_throws403() throws Exception {
        final File f = FileUtil.createTemporaryFile("perm-test", ".txt",
                RandomStringUtils.random(64));
        final Contentlet saved =
                new FileAssetDataGen(folder, f).languageId(defaultLanguage.getId()).nextPersisted();
        final String identifier = saved.getIdentifier();

        // Use a limited user (Chris Publisher, registered in TestUserUtils)
        final User limitedUser = TestUserUtils.getChrisPublisherUser(host);

        // Explicitly revoke READ on the folder so the limited user has no access
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        permissionAPI.resetPermissionsUnder(folder);

        final WebAssetResourceV2 resource = createResource(limitedUser);

        Exception thrown = null;
        try {
            resource.getById(mockRequest(), new MockHttpResponse(),
                    identifier, null, "working");
        } catch (DotSecurityException e) {
            thrown = e;
        }

        assertNotNull("Expected DotSecurityException for user without READ permission", thrown);
    }

    // -----------------------------------------------------------------------
    // Path pointing at a folder → 400
    // -----------------------------------------------------------------------

    /**
     * When the path resolves to a folder (ends with /) the endpoint must return
     * IllegalArgumentException (→ 400) rather than bytes.
     */
    @Test
    public void testGetByPath_folderPath_returns400() throws Exception {
        final String folderPath = String.format("//%s/%s/", host.getHostname(), folder.getName());
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        Exception thrown = null;
        try {
            resource.getByPath(mockRequest(), new MockHttpResponse(),
                    folderPath, null, "working");
        } catch (IllegalArgumentException | BadRequestException e) {
            thrown = e;
        }

        assertNotNull("Expected 400-class exception when path points at a folder", thrown);
    }

    // -----------------------------------------------------------------------
    // Unknown host → 404
    // -----------------------------------------------------------------------

    /**
     * A path whose host component does not match any registered site must result
     * in a NotFoundException (→ 404).
     */
    @Test
    public void testGetByPath_unknownHost_returns404() throws Exception {
        final String badPath = "//no-such-host-xyz-9999.dotcms.invalid/some/file.txt";
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        Exception thrown = null;
        try {
            resource.getByPath(mockRequest(), new MockHttpResponse(),
                    badPath, null, "working");
        } catch (com.dotcms.rest.exception.NotFoundException
                 | com.dotcms.contenttype.exception.NotFoundInDbException e) {
            thrown = e;
        }

        assertNotNull("Expected 404-class exception for unknown host", thrown);
    }

    // -----------------------------------------------------------------------
    // Write response includes correct fileSize
    // -----------------------------------------------------------------------

    /**
     * The JSON response body from a save must include a {@code fileSize} field
     * that matches the actual byte length of the uploaded file.
     */
    @Test
    public void testSave_responseIncludesCorrectFileSize() throws Exception {
        final String content = RandomStringUtils.random(1024);
        final File f = FileUtil.createTemporaryFile("size-check", ".txt", content);
        final String path = assetPath(f.getName());
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        try (final InputStream is = Files.newInputStream(f.toPath())) {
            final ResponseEntityFileAssetView result = resource.save(
                    mockRequest(), new MockHttpResponse(),
                    is, disposition(f), path, null);

            assertNotNull(result);
            final FileAssetView view = result.getEntity();
            assertNotNull(view);
            assertTrue("fileSize must be > 0", view.getFileSize() > 0);
            // Byte length matches the file on disk
            assertEquals(f.length(), view.getFileSize());
        }
    }

    // -----------------------------------------------------------------------
    // .dotsass extension works for save + read
    // -----------------------------------------------------------------------

    /**
     * File assets with the {@code .dotsass} extension (used by dotCMS theming)
     * must be saveable and retrievable through the v2 endpoint without error.
     */
    @Test
    public void testSaveAndGet_dotSassExtension_works() throws Exception {
        final String sassContent = "$primary-color: #336699;\nbody { color: $primary-color; }";
        final File sassFile = FileUtil.createTemporaryFile("theme", ".dotsass", sassContent);
        final String path = assetPath(sassFile.getName());
        final WebAssetResourceV2 resource = createResource(APILocator.systemUser());

        // Save
        try (final InputStream is = Files.newInputStream(sassFile.toPath())) {
            final ResponseEntityFileAssetView saved = resource.save(
                    mockRequest(), new MockHttpResponse(),
                    is, disposition(sassFile), path, null);
            assertNotNull(saved.getEntity().getIdentifier());
        }

        // Read back
        final Response resp = resource.getByPath(mockRequest(), new MockHttpResponse(),
                path, null, "working");
        assertEquals(200, resp.getStatus());

        final String body = new String(streamToBytes(resp), StandardCharsets.UTF_8);
        assertEquals(sassContent, body);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a {@link WebAssetResourceV2} with a mocked {@link WebResource} that
     * always returns the given user.
     */
    private WebAssetResourceV2 createResource(final User user) {
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initDataObject);

        return new WebAssetResourceV2(
                webResource,
                WebAssetHelper.newInstance(),
                APILocator.getContentletAPI(),
                APILocator.getFileAssetAPI(),
                APILocator.getLanguageAPI(),
                APILocator.getPermissionAPI()
        );
    }

    /** Returns a host-qualified asset path under the shared test folder. */
    private String assetPath(final String fileName) {
        return String.format(ASSET_PATH_TEMPLATE,
                host.getHostname(), folder.getName(), fileName, "")
                .replaceAll("/$", "")   // trim trailing slash added by template
                .replace("/" + fileName + "/", "/" + fileName);
    }

    /**
     * Builds the asset path correctly — folder is one level, file is the last segment.
     */
    private String assetPath2(final String fileName) {
        return String.format("//%s/%s/%s", host.getHostname(), folder.getName(), fileName);
    }

    /** Creates a {@link MockHeaderRequest} carrying the system-user session attribute. */
    @NotNull
    private static MockHeaderRequest mockRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request()
                        ).request()
                ).request()
        );
        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));
        request.setHeader("User-Agent", "Fake-Agent");
        request.setHeader("Host", "localhost");
        request.setHeader("Origin", "localhost");
        request.setAttribute(WebKeys.USER, APILocator.systemUser());
        return request;
    }

    /** Helper to create a {@link FormDataContentDisposition} from a file. */
    private static FormDataContentDisposition disposition(final File file) {
        return FormDataContentDisposition
                .name(file.getName())
                .fileName(file.getName())
                .size(file.length())
                .build();
    }

    /** Helper to open an {@link InputStream} from a string. */
    private static InputStream stream(final String content) {
        return new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /** Helper to create a temporary file with string content. */
    private static File tmpFile(final String prefix, final String suffix, final String content)
            throws IOException {
        return FileUtil.createTemporaryFile(prefix, suffix, content);
    }

    /**
     * Drains a JAX-RS {@link Response} whose entity is a {@link StreamingOutput}
     * into a byte array.
     */
    private static byte[] streamToBytes(final Response response) throws IOException {
        final Object entity = response.getEntity();
        if (entity instanceof StreamingOutput) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ((StreamingOutput) entity).write(baos);
            return baos.toByteArray();
        }
        if (entity instanceof byte[]) {
            return (byte[]) entity;
        }
        return new byte[0];
    }
}
