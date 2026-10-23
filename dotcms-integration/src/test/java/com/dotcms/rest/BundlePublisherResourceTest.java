package com.dotcms.rest;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.EnvironmentDataGen;
import com.dotcms.datagen.PushPublishingEndPointDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.util.ConfigUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the file name handling of {@link BundlePublisherResource#publish} when a bundle is
 * received from a registered sending endpoint.
 */
public class BundlePublisherResourceTest extends IntegrationTestBase {

    private static final String SENDER_ADDRESS = "10.20.30.40";
    private static final String SENDER_KEY = "bundle-receiver-test-key";

    private final List<File> filesToDelete = new ArrayList<>();
    private final List<File> directoriesToDelete = new ArrayList<>();
    private Environment environment;
    private PushPublishingEndPoint endpoint;
    private File bundlesDir;
    private String marker;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void registerSender() throws Exception {
        marker = "r" + (System.currentTimeMillis() % 1_000_000_000L);
        bundlesDir = new File(ConfigUtils.getBundlePath());
        environment = new EnvironmentDataGen().nextPersisted();
        endpoint = new PushPublishingEndPointDataGen()
                .environment(environment)
                .address(SENDER_ADDRESS)
                .authKey(SENDER_KEY)
                .sending(true)
                .nextPersisted();
        assertNotNull(endpoint.getId());
    }

    @After
    public void cleanUp() throws Exception {
        for (final File file : filesToDelete) {
            Files.deleteIfExists(file.toPath());
        }
        for (final File directory : directoriesToDelete) {
            FileUtils.deleteQuietly(directory);
        }
        if (endpoint != null) {
            APILocator.getPublisherEndPointAPI().deleteEndPointById(endpoint.getId());
        }
        if (environment != null) {
            APILocator.getEnvironmentAPI().deleteEnvironment(environment.getId());
        }
    }

    /**
     * Method to test: {@link BundlePublisherResource#publish}
     * When: the bundle file name carries relative path segments pointing outside of the bundles
     * directory
     * Should: reject the request and not write anything outside of the bundles directory.
     */
    @Test
    public void test_publish_doesNotWriteOutsideBundlesDirectory() throws Exception {
        final File outsideFile = new File(bundlesDir.getParentFile(), marker + ".tar.gz");
        filesToDelete.add(outsideFile);

        final Response response = publish("../" + marker + ".tar.gz");

        assertFalse("bundle must not be written outside the bundles directory: " + outsideFile,
                outsideFile.exists());
        assertEquals(400, response.getStatus());
    }

    /**
     * Method to test: {@link BundlePublisherResource#publish}
     * When: the bundle file name resolves to a sibling directory whose name starts with the name
     * of the bundles directory
     * Should: reject the request and not write anything in that directory.
     */
    @Test
    public void test_publish_rejectsNameResolvingToSiblingOfBundlesDirectory() throws Exception {
        final File sibling = new File(bundlesDir.getParentFile(), bundlesDir.getName() + "_s");
        assertTrue(sibling.mkdirs() || sibling.isDirectory());
        directoriesToDelete.add(sibling);

        final Response response = publish("../" + sibling.getName() + "/" + marker + ".tar.gz");

        assertFalse(new File(sibling, marker + ".tar.gz").exists());
        assertEquals(400, response.getStatus());
    }

    /**
     * Method to test: {@link BundlePublisherResource#publish}
     * When: the bundle file name contains a backslash
     * Should: reject the request and not create a file with that name.
     */
    @Test
    public void test_publish_rejectsNameWithBackslash() throws Exception {
        final String name = "..\\" + marker + ".tar.gz";
        filesToDelete.add(new File(bundlesDir, name));

        final Response response = publish(name);

        assertFalse(new File(bundlesDir, name).exists());
        assertEquals(400, response.getStatus());
    }

    /**
     * Method to test: {@link BundlePublisherResource#publish}
     * When: the bundle file name is an absolute path
     * Should: reject the request and not create anything below the bundles directory.
     */
    @Test
    public void test_publish_rejectsAbsolutePathName() throws Exception {
        final File absolute = new File(System.getProperty("java.io.tmpdir"), marker + ".tar.gz");
        filesToDelete.add(absolute);
        filesToDelete.add(new File(bundlesDir, absolute.getAbsolutePath()));

        final Response response = publish(absolute.getAbsolutePath());

        assertFalse(absolute.exists());
        assertEquals(400, response.getStatus());
    }

    /**
     * Method to test: {@link BundlePublisherResource#publish}
     * When: the bundle file name has a nested directory
     * Should: reject the request and not create the directory.
     */
    @Test
    public void test_publish_rejectsNestedName() throws Exception {
        final File nested = new File(bundlesDir, marker + "_dir");
        directoriesToDelete.add(nested);
        assertTrue(nested.mkdirs());

        final Response response = publish(nested.getName() + "/inner.tar.gz");

        assertFalse(new File(nested, "inner.tar.gz").exists());
        assertEquals(400, response.getStatus());
    }

    /**
     * Method to test: {@link BundlePublisherResource#publish}
     * When: the bundle file name contains a NUL character
     * Should: reject the request.
     */
    @Test
    public void test_publish_rejectsNameWithNul() throws Exception {
        final Response response = publish(marker + "\0.tar.gz");

        assertEquals(400, response.getStatus());
    }

    /**
     * Method to test: {@link BundlePublisherResource#publish}
     * When: the bundle file name does not end in .tar.gz
     * Should: reject the request with a client error and not create a bundle record.
     */
    @Test
    public void test_publish_rejectsNameWithoutBundleExtension() throws Exception {
        filesToDelete.add(new File(bundlesDir, marker));

        final Response response = publish(marker);

        assertNull(bundleRecord(marker));
        assertEquals(400, response.getStatus());
    }

    /**
     * Method to test: {@link BundlePublisherResource#publish}
     * When: the bundle file name has nothing before the .tar.gz extension
     * Should: reject the request and not create a file.
     */
    @Test
    public void test_publish_rejectsNameWithEmptyStem() throws Exception {
        final File hidden = new File(bundlesDir, ".tar.gz");
        assertFalse("test precondition: no leftover file", hidden.exists());
        filesToDelete.add(hidden);

        final Response response = publish(".tar.gz");

        assertFalse(hidden.exists());
        assertEquals(400, response.getStatus());
    }

    /**
     * Method to test: {@link BundlePublisherResource#publish}
     * When: the bundle file name is two leading dots followed by the extension, so the part
     * before the extension is {@code .}
     * Should: reject the request and not create a bundle record for {@code .}.
     */
    @Test
    public void test_publish_rejectsNameWhoseIdIsADot() throws Exception {
        final Response response = publish("..tar.gz");

        assertNull(bundleRecord("."));
        assertEquals(400, response.getStatus());
    }

    /**
     * Method to test: {@link BundlePublisherResource#publish}
     * When: the bundle file name is three leading dots followed by the extension, so the part
     * before the extension is {@code ..}
     * Should: reject the request and not create a bundle record for {@code ..}.
     */
    @Test
    public void test_publish_rejectsNameWhoseIdIsDotDot() throws Exception {
        final Response response = publish("...tar.gz");

        assertNull(bundleRecord(".."));
        assertEquals(400, response.getStatus());
    }

    /**
     * Method to test: {@link BundlePublisherResource#publish}
     * When: the bundle file name is a plain id followed by .tar.gz, as senders send it
     * Should: accept the bundle and register it.
     */
    @Test
    public void test_publish_acceptsPlainBundleName() throws Exception {
        final String bundleId = UUID.randomUUID().toString();
        filesToDelete.add(new File(bundlesDir, bundleId + ".tar.gz"));

        final Response response = publish(bundleId + ".tar.gz");

        try {
            assertEquals(200, response.getStatus());
            assertNotNull(bundleRecord(bundleId));
        } finally {
            if (bundleRecord(bundleId) != null) {
                APILocator.getBundleAPI().deleteBundle(bundleId);
            }
        }
    }

    private Response publish(final String fileName) throws Exception {
        final HttpServletRequest request = bundleRequest(fileName);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        return new BundlePublisherResource().publish("plain", null, false, null, request, response);
    }

    private Bundle bundleRecord(final String bundleId) throws Exception {
        final Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);
        return bundle == null || bundle.getId() == null ? null : bundle;
    }

    private HttpServletRequest bundleRequest(final String fileName) throws IOException {
        final byte[] payload = "not-a-real-bundle".getBytes(StandardCharsets.UTF_8);
        final ByteArrayInputStream body = new ByteArrayInputStream(payload);
        final ServletInputStream stream = new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
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
                return body.read();
            }
        };

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getInputStream()).thenReturn(stream);
        when(request.getRemoteHost()).thenReturn(SENDER_ADDRESS);
        when(request.getRemoteAddr()).thenReturn(SENDER_ADDRESS);
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + PublicEncryptionFactory.digestString(SENDER_KEY));
        when(request.getHeader("Content-Disposition"))
                .thenReturn("attachment; filename=\"" + fileName + "\"");
        return request;
    }
}
