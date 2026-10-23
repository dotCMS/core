package com.dotcms.publisher.receiver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.EnvironmentDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.PushPublishingEndPointDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.response.MockAsyncResponse;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.publisher.ajax.RemotePublishAjaxAction;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.rest.BundlePublisherResource;
import com.dotcms.rest.BundleResource;
import com.dotcms.rest.PushPublisherJob;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.util.ConfigUtils;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import io.vavr.control.Try;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.felix.framework.OSGIUtil;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Tests who may bring plugin jars in through a received bundle: the access rules of the upload
 * endpoints (REST sync, REST async and the legacy action), the bundle received from a registered
 * sender, and a bundle whose metadata cannot be read.
 *
 * <p>Plugin jars in these tests are plain text files with unique names, removed in
 * {@link #cleanup()}.
 */
public class BundlePluginDeploymentTest extends IntegrationTestBase {

    private static final String SENDER_ADDRESS = "10.20.30.50";
    private static final String SENDER_KEY = "plugin-deployment-test-key";
    private static final String UNREADABLE_METADATA = "<com.dotcms.publisher.pusher.PushPublisherConfig/>";

    private static BundleResource bundleResource;
    private static HttpServletResponse response;

    private final List<User> users = new ArrayList<>();
    private final List<Layout> layouts = new ArrayList<>();
    private final List<File> toDelete = new ArrayList<>();
    private final List<String> auditIds = new ArrayList<>();
    private Environment environment;
    private PushPublishingEndPoint endpoint;
    private File uploadFolder;
    private File deployFolder;
    private File bundlesDir;
    private String id;
    private String jarName;

    @FunctionalInterface
    private interface Call {
        void run() throws Exception;
    }

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        bundleResource = new BundleResource();
        response = new MockHttpResponse();
    }

    @Before
    public void setUp() {
        id = "zz-plugin-" + (System.nanoTime() % 1_000_000_000L);
        jarName = id + ".jar";
        uploadFolder = new File(OSGIUtil.getInstance().getFelixUploadPath());
        deployFolder = new File(OSGIUtil.getInstance().getFelixDeployPath());
        uploadFolder.mkdirs();
        deployFolder.mkdirs();
        bundlesDir = new File(ConfigUtils.getBundlePath());
        toDelete.add(new File(uploadFolder, jarName));
        toDelete.add(new File(deployFolder, jarName));
        toDelete.add(new File(bundlesDir, id + ".tar.gz"));
        toDelete.add(new File(bundlesDir, id));
        auditIds.add(id);
    }

    @After
    public void cleanup() throws Exception {
        toDelete.forEach(FileUtils::deleteQuietly);
        for (final String auditId : auditIds) {
            Try.run(() -> PublishAuditAPI.getInstance().deletePublishAuditStatus(auditId));
            Try.run(() -> APILocator.getBundleAPI().deleteBundle(auditId));
        }
        for (final User user : users) {
            Try.run(() -> UserDataGen.remove(user, Boolean.TRUE));
        }
        for (final Layout layout : layouts) {
            Try.run(() -> APILocator.getLayoutAPI().removeLayout(layout));
        }
        if (endpoint != null) {
            Try.run(() -> APILocator.getPublisherEndPointAPI().deleteEndPointById(endpoint.getId()));
        }
        if (environment != null) {
            Try.run(() -> APILocator.getEnvironmentAPI().deleteEnvironment(environment.getId()));
        }
    }

    // ---------------------------------------------------------------- upload endpoints

    /**
     * Method to Test: {@link BundleResource#uploadBundleSync} and
     * {@link BundleResource#uploadBundleAsync}
     * Given: a back-end user without the publishing queue portlet
     * When: they upload a bundle on either endpoint
     * Should: be rejected with 401, the same answer as the sibling endpoints, and no archive is
     * written to the bundles folder.
     */
    @Test
    public void test_upload_withoutPublishingQueuePortlet_isRejectedWith401() throws Exception {
        final User user = backendUserWithPortlets();
        final byte[] archive = bundle(jarName, "osgiBundles/", false);

        assertEquals(401, statusOf(() -> uploadSync(user, archive)));
        assertEquals(401, statusOf(() -> uploadAsync(user, archive)));
        assertFalse("no archive may be written for a rejected upload",
                new File(bundlesDir, id + ".tar.gz").exists());
    }

    /**
     * Method to Test: {@link BundleResource#uploadBundleSync} and
     * {@link BundleResource#uploadBundleAsync}
     * Given: a back-end user with the publishing queue portlet but without the plugins portlet
     * When: they upload a bundle that carries a plugin jar on either endpoint
     * Should: be rejected with 401, no jar reaches the plugin folders, and the stored archive is
     * removed.
     */
    @Test
    public void test_upload_withPluginsAndWithoutPluginsPortlet_isRejectedWith401() throws Exception {
        final User user = backendUserWithPortlets("publishing-queue");
        final byte[] archive = bundle(jarName, "osgiBundles/", true);

        assertEquals(401, statusOf(() -> uploadSync(user, archive)));
        assertNoJarAnywhere();
        assertEquals(401, statusOf(() -> uploadAsync(user, archive)));
        assertNoJarAnywhere();
    }

    /**
     * Method to Test: {@link BundleResource#uploadBundleSync}
     * Given: a back-end user with the publishing queue portlet but without the plugins portlet
     * When: the bundle names its plugin folder through a parent segment that still resolves into
     * it, as in {@code x/../osgiBundles/<jar>}
     * Should: be rejected with 401 the same way, and no jar reaches the plugin folders.
     */
    @Test
    public void test_upload_withPluginsInsideAParentSegment_isRejectedWith401() throws Exception {
        final User user = backendUserWithPortlets("publishing-queue");
        final byte[] archive = bundle(jarName, "x/../osgiBundles/", true);

        assertEquals(401, statusOf(() -> uploadSync(user, archive)));
        assertNoJarAnywhere();
    }

    /**
     * Method to Test: {@link BundleResource#uploadBundleSync}
     * Given: a back-end user with both the publishing queue and the plugins portlets
     * When: they upload a bundle that carries a plugin jar
     * Should: not be rejected by the access rules, so legitimate uploads keep working.
     */
    @Test
    public void test_upload_withBothPortlets_isNotRejected() throws Exception {
        final User user = backendUserWithPortlets("publishing-queue", "dynamic-plugins");

        assertEquals(200, statusOf(() -> uploadSync(user, bundle(jarName, "osgiBundles/", true))));
    }

    // ---------------------------------------------------------------- legacy upload action

    /**
     * Method to Test: {@link RemotePublishAjaxAction#uploadBundle(HttpServletRequest, HttpServletResponse)}
     * Given: a back-end user with the publishing queue portlet but without the plugins portlet
     * When: they upload a bundle that carries a plugin jar through the legacy action
     * Should: answer 401, remove the stored archive, deploy no jar, and leave no entry in the
     * publishing history.
     */
    @Test
    public void test_legacyUpload_withPluginsAndWithoutPluginsPortlet_isRejectedBeforeAudit()
            throws Exception {
        final User user = backendUserWithPortlets("publishing-queue");
        final HttpServletResponse legacyResponse = mock(HttpServletResponse.class);
        when(legacyResponse.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        final RemotePublishAjaxAction action = new RemotePublishAjaxAction();
        action.setUser(user);

        action.uploadBundle(multipartRequest(id + ".tar.gz", bundle(jarName, "osgiBundles/", true)),
                legacyResponse);

        verify(legacyResponse).sendError(401);
        assertNoJarAnywhere();
        assertEquals("a rejected upload must not leave an entry in the publishing history", null,
                PublishAuditAPI.getInstance().getPublishAuditStatus(id));
    }

    // ---------------------------------------------------------------- registered sender

    /**
     * Method to Test: {@link BundlePublisherResource#publish} and
     * {@link PushPublisherJob#processBundle(String, PublishAuditStatus, BundleOrigin, String)}
     * Given: a bundle received from a registered sending endpoint that carries a plugin jar
     * When: it is published and the queued job then processes it
     * Should: queue the job as coming from a registered sender and, when processed that way,
     * still deploy the jar, so pushing plugins between servers keeps working.
     */
    @Test
    public void test_receivedFromRegisteredSender_stillDeploysPluginJar() throws Exception {
        environment = new EnvironmentDataGen().nextPersisted();
        endpoint = new PushPublishingEndPointDataGen()
                .environment(environment)
                .address(SENDER_ADDRESS)
                .authKey(SENDER_KEY)
                .sending(true)
                .nextPersisted();

        final ArgumentCaptor<PublishAuditStatus> status = ArgumentCaptor.forClass(PublishAuditStatus.class);
        final ArgumentCaptor<BundleOrigin> origin = ArgumentCaptor.forClass(BundleOrigin.class);
        final ArgumentCaptor<String> uploader = ArgumentCaptor.forClass(String.class);
        final Response result;
        try (MockedStatic<PushPublisherJob> job = Mockito.mockStatic(PushPublisherJob.class)) {
            result = new BundlePublisherResource().publish("plain", null, false, null,
                    senderRequest(id + ".tar.gz", bundle(jarName, "osgiBundles/", true)),
                    mock(HttpServletResponse.class));
            job.verify(() -> PushPublisherJob.triggerPushPublisherJob(eq(id + ".tar.gz"),
                    status.capture(), origin.capture(), uploader.capture()));
        }

        assertEquals(200, result.getStatus());
        assertEquals(BundleOrigin.REGISTERED_SENDER, origin.getValue());

        new PushPublisherJob().processBundle(id + ".tar.gz", status.getValue(), origin.getValue(),
                uploader.getValue());

        assertTrue("the jar must be deployed",
                new File(uploadFolder, jarName).exists() || new File(deployFolder, jarName).exists());
    }

    // ---------------------------------------------------------------- unreadable metadata

    /**
     * Method to Test: {@link BundlePublisher#process(PublishStatus)}
     * When: the metadata file of a received bundle cannot be read, even though the sender may
     * deploy plugins, and the bundle carries a plugin jar
     * Should: stop before any handler runs (nothing is copied to the plugin upload folder), mark
     * the bundle as failed, and remove both the extracted folder and the received archive.
     */
    @Test
    public void test_process_unreadableMetadata_abortsBeforeHandlersAndCleansUp() throws Exception {
        LicenseTestUtil.getLicense();
        final File bundleFile = new File(bundlesDir, id + ".tar.gz");
        final File extractedFolder = new File(bundlesDir, id);

        final PublishAuditAPI audit = PublishAuditAPI.getInstance();
        final PublishAuditStatus status = new PublishAuditStatus(id);
        final PublishAuditHistory history = new PublishAuditHistory();
        history.addOrUpdateEndpoint("endpoint1", "endpoint1", null);
        status.setStatusPojo(history);
        audit.insertPublishAuditStatus(status);

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setId(id + ".tar.gz");
        config.setPublishAuditStatus(status);
        config.setAssets(new ArrayList<>());
        config.put(PublisherConfig.Config.BUNDLE_ORIGIN.name(), BundleOrigin.REGISTERED_SENDER.name());

        FileUtils.writeByteArrayToFile(bundleFile,
                tarGz(entries(UNREADABLE_METADATA.getBytes(StandardCharsets.UTF_8), jarName, "osgiBundles/")));

        final BundlePublisher publisher = new BundlePublisher();
        publisher.init(config);
        try {
            publisher.process(new PublishStatus());
            org.junit.Assert.fail("A bundle whose metadata cannot be read must fail");
        } catch (final DotPublishingException expected) {
            // expected
        }

        assertNoJarAnywhere();
        assertEquals(PublishAuditStatus.Status.FAILED_TO_PUBLISH, audit.getPublishAuditStatus(id).getStatus());
        assertFalse("the extracted folder must be removed", extractedFolder.exists());
        assertFalse("the received archive must be removed", bundleFile.exists());
    }

    // ---------------------------------------------------------------- helpers

    private void assertNoJarAnywhere() {
        assertFalse("no jar may reach the plugin upload folder", new File(uploadFolder, jarName).exists());
        assertFalse("no jar may reach the plugin load folder", new File(deployFolder, jarName).exists());
        assertFalse("the stored archive must be removed", new File(bundlesDir, id + ".tar.gz").exists());
    }

    private User backendUserWithPortlets(final String... portletIds) throws Exception {
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
        users.add(user);
        if (portletIds.length > 0) {
            final Layout layout = new LayoutDataGen().portletIds(portletIds).nextPersisted();
            layouts.add(layout);
            APILocator.getRoleAPI().addLayoutToRole(layout, APILocator.getRoleAPI().getUserRole(user));
        }
        return user;
    }

    private static int statusOf(final Call call) throws Exception {
        try {
            call.run();
            return 200;
        } catch (final WebApplicationException e) {
            return e.getResponse().getStatus();
        }
    }

    private void uploadSync(final User user, final byte[] archive) throws DotPublisherException {
        bundleResource.uploadBundleSync(requestFor(user), response, multipart(id + ".tar.gz", archive));
    }

    private void uploadAsync(final User user, final byte[] archive) throws DotPublisherException {
        bundleResource.uploadBundleAsync(requestFor(user), response,
                new MockAsyncResponse(arg -> true, arg -> true), multipart(id + ".tar.gz", archive));
    }

    private static HttpServletRequest requestFor(final User user) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        return request;
    }

    private static FormDataMultiPart multipart(final String fileName, final byte[] content) {
        final BodyPart bodyPart = mock(BodyPart.class);
        when(bodyPart.getEntity()).thenReturn(new ByteArrayInputStream(content));
        final ContentDisposition meta = mock(ContentDisposition.class);
        when(bodyPart.getContentDisposition()).thenReturn(meta);
        when(meta.getFileName()).thenReturn(fileName);
        final FormDataMultiPart multipart = mock(FormDataMultiPart.class);
        when(multipart.getBodyParts()).thenReturn(java.util.Collections.singletonList(bodyPart));
        return multipart;
    }

    private static HttpServletRequest multipartRequest(final String fileName, final byte[] content)
            throws IOException {
        final String boundary = "----testboundary";
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                + fileName + "\"\r\nContent-Type: application/gzip\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        final byte[] bytes = body.toByteArray();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary);
        when(request.getContentLength()).thenReturn(bytes.length);
        when(request.getContentLengthLong()).thenReturn((long) bytes.length);
        when(request.getCharacterEncoding()).thenReturn("UTF-8");
        when(request.getInputStream()).thenReturn(servletStream(bytes));
        return request;
    }

    private HttpServletRequest senderRequest(final String fileName, final byte[] content)
            throws IOException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getInputStream()).thenReturn(servletStream(content));
        when(request.getRemoteHost()).thenReturn(SENDER_ADDRESS);
        when(request.getRemoteAddr()).thenReturn(SENDER_ADDRESS);
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + PublicEncryptionFactory.digestString(SENDER_KEY));
        when(request.getHeader("Content-Disposition"))
                .thenReturn("attachment; filename=\"" + fileName + "\"");
        return request;
    }

    private static ServletInputStream servletStream(final byte[] bytes) {
        final ByteArrayInputStream source = new ByteArrayInputStream(bytes);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return source.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(final ReadListener readListener) {
            }

            @Override
            public int read() {
                return source.read();
            }
        };
    }

    /** A bundle with readable metadata and, optionally, a plugin jar under the given folder prefix. */
    private static byte[] bundle(final String jar, final String folderPrefix, final boolean withPlugin)
            throws IOException {
        final ByteArrayOutputStream xml = new ByteArrayOutputStream();
        BundlerUtil.objectToXML(new PushPublisherConfig(), xml);
        final Map<String, byte[]> entries = entries(xml.toByteArray(), withPlugin ? jar : null, folderPrefix);
        return tarGz(entries);
    }

    private static Map<String, byte[]> entries(final byte[] metadata, final String jar,
            final String folderPrefix) {
        final Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("bundle.xml", metadata);
        if (jar != null) {
            entries.put(folderPrefix + jar, "not a real jar".getBytes(StandardCharsets.UTF_8));
            entries.put(folderPrefix + jar + ".descriptor.osgi.json",
                    ("{\"operation\":\"PUBLISH\",\"jarName\":\"" + jar + "\"}")
                            .getBytes(StandardCharsets.UTF_8));
        }
        return entries;
    }

    private static byte[] tarGz(final Map<String, byte[]> entries) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GZIPOutputStream(bytes))) {
            for (final Map.Entry<String, byte[]> item : entries.entrySet()) {
                final TarArchiveEntry entry = new TarArchiveEntry(item.getKey());
                entry.setSize(item.getValue().length);
                tar.putArchiveEntry(entry);
                tar.write(item.getValue());
                tar.closeArchiveEntry();
            }
        }
        return bytes.toByteArray();
    }
}
