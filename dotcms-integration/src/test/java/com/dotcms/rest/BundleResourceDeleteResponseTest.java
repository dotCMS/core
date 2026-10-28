package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotConcurrentFactory.SubmitterConfigBuilder;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the synchronous response of {@link BundleResource#deleteBundlesByIdentifiers}, whose
 * actual delete is submitted to a background thread pool.
 */
public class BundleResourceDeleteResponseTest {

    private static final List<String> createdBundleIds = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        restoreBundlePool();
        for (final String bundleId : createdBundleIds) {
            try {
                APILocator.getBundleAPI().deleteBundleAndDependencies(bundleId, APILocator.systemUser());
            } catch (Exception e) {
                // already removed by the delete under test
            }
        }
        createdBundleIds.clear();
    }

    /**
     * Method to Test: {@link BundleResource#deleteBundlesByIdentifiers(HttpServletRequest, HttpServletResponse, DeleteBundlesByIdentifierForm)}
     * Given: several existing bundles and one identifier that never existed
     * When:  they are deleted in one request
     * Should: report the existing bundles as being removed and only the unknown identifier as
     *         not existing, even when the delete has already finished by the time the response
     *         is built. The bundle pool is reduced to one busy thread with a caller-runs policy,
     *         so the delete runs on the request thread before the endpoint returns.
     */
    @Test
    public void test_deleteBundlesByIdentifiers_responseReflectsStateBeforeDelete() throws Exception {
        final List<String> existingIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            existingIds.add(insertBundle());
        }
        final String unknownId = UUIDGenerator.generateUuid();
        final List<String> identifiers = new ArrayList<>(existingIds);
        identifiers.add(unknownId);

        final CountDownLatch release = new CountDownLatch(1);
        final Response response;
        try {
            occupyBundlePool(release);
            response = new BundleResource().deleteBundlesByIdentifiers(
                    getAdminRequest(), mock(HttpServletResponse.class),
                    new DeleteBundlesByIdentifierForm.Builder().identifiers(identifiers).build());
        } finally {
            release.countDown();
        }

        for (final String id : existingIds) {
            assertNull("the delete should have run before the response was returned: " + id,
                    APILocator.getBundleAPI().getBundleById(id));
        }
        assertEquals(200, response.getStatus());
        final String message = ((ResponseEntityView<?>) response.getEntity()).getEntity().toString();
        assertTrue(message, message.startsWith("Removing the follow bundles in a separated process, "
                + "the result of the operation will be notified: " + String.join(", ", existingIds)));
        assertTrue(message, message.endsWith(
                "The following bundles were not deleted because they don't exist: " + unknownId));
    }

    /**
     * Replaces the bundle pool with a single thread that has no queue and runs rejected tasks on
     * the caller's thread, then keeps that thread busy until {@code release} is counted down.
     */
    private static void occupyBundlePool(final CountDownLatch release) {
        final DotConcurrentFactory factory = DotConcurrentFactory.getInstance();
        factory.shutdown(BundleResource.BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        factory.getSubmitter(BundleResource.BUNDLE_THREAD_POOL_SUBMITTER_NAME,
                new SubmitterConfigBuilder().poolSize(1).maxPoolSize(1).queueCapacity(0).build())
                .execute(() -> {
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
    }

    /**
     * Drops the pool and the creator registered by {@link #occupyBundlePool(CountDownLatch)} so
     * that other tests in the same JVM get the default bundle pool again. The factory has no API
     * to unregister a creator.
     */
    private static void restoreBundlePool() throws Exception {
        final DotConcurrentFactory factory = DotConcurrentFactory.getInstance();
        factory.shutdown(BundleResource.BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        final Field creators = DotConcurrentFactory.class.getDeclaredField("submitterConfigCreatorMap");
        creators.setAccessible(true);
        ((Map<?, ?>) creators.get(factory)).remove(BundleResource.BUNDLE_THREAD_POOL_SUBMITTER_NAME);
    }

    private static HttpServletRequest getAdminRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));
        return request;
    }

    private static String insertBundle() throws DotDataException {
        final Bundle bundle = new Bundle();
        bundle.setId(UUIDGenerator.generateUuid());
        bundle.setName("deleteResponseTestBundle" + System.currentTimeMillis());
        bundle.setForcePush(false);
        bundle.setOwner(APILocator.systemUser().getUserId());
        bundle.setPublishDate(new Date());
        APILocator.getBundleAPI().saveBundle(bundle);
        createdBundleIds.add(bundle.getId());
        return bundle.getId();
    }
}
