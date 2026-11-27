package com.dotcms.publisher.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.LicenseTestUtil;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.business.PublisherTestUtil.MixedEndpointEnvironment;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.dotcms.util.IntegrationTestInitService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Checks the audit ({@code publishing_queue_audit}) of a bundle published to an environment that has
 * an unreachable {@code http} endpoint and a {@code static} endpoint: every endpoint must be listed
 * with its own status, whichever publisher runs first (issue #34434).
 */
public class PublishAuditMixedEndpointsTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    @SafeVarargs
    private static List<Class<? extends Publisher>> order(final Class<? extends Publisher>... publishers) {
        return Arrays.asList(publishers);
    }

    private static Map<String, EndpointDetail> endpointsOf(final MixedEndpointEnvironment fixture)
            throws Exception {
        final PublishAuditHistory history = PublishAuditAPI.getInstance()
                .getPublishAuditStatus(fixture.bundle.getId()).getStatusPojo();
        final Map<String, EndpointDetail> endpoints =
                history.getEndpointsMap().get(fixture.environment.getId());
        assertNotNull("The audit has no entry for the environment", endpoints);
        return endpoints;
    }

    private void assertBothEndpointsRecorded(final List<Class<? extends Publisher>> publishers,
            final boolean forcePush) throws Exception {
        final MixedEndpointEnvironment fixture =
                PublisherTestUtil.createMixedEndpointEnvironment(publishers, forcePush);
        try {
            new PublisherAPIImpl().publish(fixture.config);

            final Map<String, EndpointDetail> endpoints = endpointsOf(fixture);
            assertTrue("The http endpoint is missing from the audit for " + publishers
                            + " (force push " + forcePush + "): " + endpoints.keySet(),
                    endpoints.containsKey(fixture.httpEndpoint.getId()));
            assertTrue("The static endpoint is missing from the audit for " + publishers
                            + " (force push " + forcePush + "): " + endpoints.keySet(),
                    endpoints.containsKey(fixture.staticEndpoint.getId()));
            assertEquals(PublishAuditStatus.Status.FAILED_TO_SENT.getCode(),
                    endpoints.get(fixture.httpEndpoint.getId()).getStatus());
            assertEquals(PublishAuditStatus.Status.BUNDLE_SAVED_SUCCESSFULLY.getCode(),
                    endpoints.get(fixture.staticEndpoint.getId()).getStatus());
        } finally {
            fixture.cleanUp();
        }
    }

    /**
     * Method to test: {@link PublisherAPIImpl#publish(com.dotcms.publishing.PublisherConfig)}
     * Given scenario: http + static endpoints, push publisher first, Default (no force push)
     * Expected result: both endpoints are in the audit, the http one FAILED and the static one saved.
     * Before the fix the static endpoint is lost because the push publisher already wrote the audit.
     */
    @Test
    public void test_audit_pushThenStatic_default_listsBothEndpoints() throws Exception {
        assertBothEndpointsRecorded(order(PushPublisher.class, StaticPublisher.class), false);
    }

    /**
     * Method to test: same as above
     * Given scenario: static publisher first, Default
     * Expected result: both endpoints are in the audit (a guard: this order already worked)
     */
    @Test
    public void test_audit_staticThenPush_default_listsBothEndpoints() throws Exception {
        assertBothEndpointsRecorded(order(StaticPublisher.class, PushPublisher.class), false);
    }

    /**
     * Method to test: same as above
     * Given scenario: push publisher first, bundle marked "Force Push"
     * Expected result: both endpoints are in the audit
     */
    @Test
    public void test_audit_pushThenStatic_forcePush_listsBothEndpoints() throws Exception {
        assertBothEndpointsRecorded(order(PushPublisher.class, StaticPublisher.class), true);
    }

    /**
     * Method to test: same as above
     * Given scenario: static publisher first, bundle marked "Force Push"
     * Expected result: both endpoints are in the audit
     */
    @Test
    public void test_audit_staticThenPush_forcePush_listsBothEndpoints() throws Exception {
        assertBothEndpointsRecorded(order(StaticPublisher.class, PushPublisher.class), true);
    }

    /**
     * Method to test: {@link PushPublisher#process(com.dotcms.publishing.PublishStatus)} with
     * {@link DeliveryStrategy#FAILED_ENDPOINTS}
     * Given scenario: a retry pass on an audit that lists the environment with only the static
     * endpoint, so the {@code http} endpoint has no entry
     * Expected result: no exception, and the {@code http} endpoint is attempted and recorded.
     * Before the fix {@code PushPublisher.process} dereferences the missing entry and throws a
     * {@link NullPointerException}.
     */
    @Test
    public void test_retry_endpointWithoutEntry_isSentAndRecorded() throws Exception {
        final MixedEndpointEnvironment fixture = PublisherTestUtil.createMixedEndpointEnvironment(
                order(PushPublisher.class), false);
        try {
            final PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();
            final PublishAuditHistory history =
                    auditAPI.getPublishAuditStatus(fixture.bundle.getId()).getStatusPojo();
            history.setNumTries(1);
            history.addOrUpdateEndpoint(fixture.environment.getId(), fixture.staticEndpoint.getId(),
                    detail(PublishAuditStatus.Status.BUNDLE_SAVED_SUCCESSFULLY.getCode(), "saved"));
            auditAPI.updatePublishAuditStatus(fixture.bundle.getId(),
                    PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS, history);
            fixture.config.setDeliveryStrategy(DeliveryStrategy.FAILED_ENDPOINTS);

            new PublisherAPIImpl().publish(fixture.config);

            final Map<String, EndpointDetail> endpoints = endpointsOf(fixture);
            assertTrue("The http endpoint must be attempted and recorded: " + endpoints.keySet(),
                    endpoints.containsKey(fixture.httpEndpoint.getId()));
            assertEquals("The static endpoint's earlier result must stay",
                    PublishAuditStatus.Status.BUNDLE_SAVED_SUCCESSFULLY.getCode(),
                    endpoints.get(fixture.staticEndpoint.getId()).getStatus());
        } finally {
            fixture.cleanUp();
        }
    }

    /**
     * Method to test: {@link StaticPublisher#process(com.dotcms.publishing.PublishStatus)}
     * Given scenario: a retry where the static endpoint already has a SUCCESS entry (merged earlier
     * by the queue job) and the static publisher succeeds again
     * Expected result: the earlier result is kept, not replaced by this run's result (AC-002)
     */
    @Test
    public void test_retry_existingSuccessEntry_isKept() throws Exception {
        final MixedEndpointEnvironment fixture = PublisherTestUtil.createMixedEndpointEnvironment(
                order(StaticPublisher.class), false);
        try {
            final PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();
            final PublishAuditHistory history =
                    auditAPI.getPublishAuditStatus(fixture.bundle.getId()).getStatusPojo();
            history.setNumTries(1);
            history.addOrUpdateEndpoint(fixture.environment.getId(), fixture.staticEndpoint.getId(),
                    detail(PublishAuditStatus.Status.SUCCESS.getCode(), "kept earlier result"));
            auditAPI.updatePublishAuditStatus(fixture.bundle.getId(),
                    PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS, history);

            new PublisherAPIImpl().publish(fixture.config);

            final EndpointDetail detail = endpointsOf(fixture).get(fixture.staticEndpoint.getId());
            assertEquals(PublishAuditStatus.Status.SUCCESS.getCode(), detail.getStatus());
            assertEquals("kept earlier result", detail.getInfo());
        } finally {
            fixture.cleanUp();
        }
    }

    /**
     * Method to test: {@link StaticPublisher#process(com.dotcms.publishing.PublishStatus)}
     * Given scenario: an earlier publisher of the same run (S3) already recorded a failed endpoint
     * in the audit, and the static publisher then succeeds on its own endpoint
     * Expected result: the overall status says that some endpoints failed
     * ({@code FAILED_TO_SEND_TO_SOME_GROUPS}) instead of {@code BUNDLE_SAVED_SUCCESSFULLY}. Today the
     * static publisher only counts its own environments, so the failed S3 endpoint is hidden behind
     * a saved status (issue #34434, review of PR 38578).
     */
    @Test
    public void test_static_earlierEndpointFailed_overallStatusIsNotSaved() throws Exception {
        final MixedEndpointEnvironment fixture = PublisherTestUtil.createMixedEndpointEnvironment(
                order(StaticPublisher.class), false);
        try {
            final PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();
            final PublishAuditHistory history =
                    auditAPI.getPublishAuditStatus(fixture.bundle.getId()).getStatusPojo();
            history.addOrUpdateEndpoint(fixture.environment.getId(), fixture.httpEndpoint.getId(),
                    detail(PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode(), "S3 upload failed"));
            auditAPI.updatePublishAuditStatus(fixture.bundle.getId(),
                    PublishAuditStatus.Status.SENDING_TO_ENDPOINTS, history);

            new PublisherAPIImpl().publish(fixture.config);

            final PublishAuditStatus.Status overall = auditAPI
                    .getPublishAuditStatus(fixture.bundle.getId()).getStatus();
            assertEquals("The failed S3 endpoint must not be hidden behind a saved status",
                    PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS, overall);
        } finally {
            fixture.cleanUp();
        }
    }

    /**
     * Method to test: {@link StaticPublisher#process(com.dotcms.publishing.PublishStatus)}
     * Given scenario: the audit keeps a FAILED entry of an endpoint that was deleted after the
     * failure, and every static endpoint that still exists now receives the bundle
     * Expected result: the deleted endpoint's old failure does not hold the bundle back, so the
     * overall status is saved. Before the fix the bundle stayed queued until the retries ran out
     * (issue #34434, review of PR 38578).
     */
    @Test
    public void test_static_failedEntryOfDeletedEndpoint_doesNotBlockSavedStatus() throws Exception {
        final MixedEndpointEnvironment fixture = PublisherTestUtil.createMixedEndpointEnvironment(
                order(StaticPublisher.class), false);
        try {
            final PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();
            final PublishAuditHistory history =
                    auditAPI.getPublishAuditStatus(fixture.bundle.getId()).getStatusPojo();
            history.addOrUpdateEndpoint(fixture.environment.getId(), "deleted-s3-endpoint",
                    detail(PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode(), "S3 upload failed"));
            auditAPI.updatePublishAuditStatus(fixture.bundle.getId(),
                    PublishAuditStatus.Status.SENDING_TO_ENDPOINTS, history);

            new PublisherAPIImpl().publish(fixture.config);

            assertEquals(PublishAuditStatus.Status.BUNDLE_SAVED_SUCCESSFULLY,
                    auditAPI.getPublishAuditStatus(fixture.bundle.getId()).getStatus());
            assertEquals("The historical entry stays in the audit for display",
                    PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode(),
                    endpointsOf(fixture).get("deleted-s3-endpoint").getStatus());
        } finally {
            fixture.cleanUp();
        }
    }

    /**
     * Method to test: {@link StaticPublisher#process(com.dotcms.publishing.PublishStatus)}
     * Given scenario: the audit keeps a FAILED entry of an endpoint that still exists but was
     * disabled afterwards, so it is no longer a delivery target
     * Expected result: the disabled endpoint's old failure does not hold the bundle back, so the
     * overall status is saved (issue #34434, review of PR 38578).
     */
    @Test
    public void test_static_failedEntryOfDisabledEndpoint_doesNotBlockSavedStatus() throws Exception {
        final MixedEndpointEnvironment fixture = PublisherTestUtil.createMixedEndpointEnvironment(
                order(StaticPublisher.class), false);
        try {
            fixture.httpEndpoint.setEnabled(false);
            APILocator.getPublisherEndPointAPI().updateEndPoint(fixture.httpEndpoint);
            final PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();
            final PublishAuditHistory history =
                    auditAPI.getPublishAuditStatus(fixture.bundle.getId()).getStatusPojo();
            history.addOrUpdateEndpoint(fixture.environment.getId(), fixture.httpEndpoint.getId(),
                    detail(PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode(), "S3 upload failed"));
            auditAPI.updatePublishAuditStatus(fixture.bundle.getId(),
                    PublishAuditStatus.Status.SENDING_TO_ENDPOINTS, history);

            new PublisherAPIImpl().publish(fixture.config);

            assertEquals(PublishAuditStatus.Status.BUNDLE_SAVED_SUCCESSFULLY,
                    auditAPI.getPublishAuditStatus(fixture.bundle.getId()).getStatus());
        } finally {
            fixture.cleanUp();
        }
    }

    /**
     * Method to test: {@link StaticPublisher#process(com.dotcms.publishing.PublishStatus)}
     * Given scenario: a retry where the static endpoint has a FAILED entry from the earlier attempt
     * and the static publisher now succeeds
     * Expected result: the endpoint's entry is replaced by this run's success and the overall status
     * is saved. Before the fix the old failure stayed, so the bundle kept reporting failed and stayed
     * queued until the retries ran out (issue #34434, review of PR 38578).
     */
    @Test
    public void test_retry_staticFailedEarlier_nowSucceeds_failureIsReplaced() throws Exception {
        final MixedEndpointEnvironment fixture = PublisherTestUtil.createMixedEndpointEnvironment(
                order(StaticPublisher.class), false);
        try {
            final PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();
            final PublishAuditHistory history =
                    auditAPI.getPublishAuditStatus(fixture.bundle.getId()).getStatusPojo();
            history.setNumTries(1);
            history.addOrUpdateEndpoint(fixture.environment.getId(), fixture.staticEndpoint.getId(),
                    detail(PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode(), "earlier failure"));
            auditAPI.updatePublishAuditStatus(fixture.bundle.getId(),
                    PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS, history);

            new PublisherAPIImpl().publish(fixture.config);

            assertEquals(PublishAuditStatus.Status.BUNDLE_SAVED_SUCCESSFULLY.getCode(),
                    endpointsOf(fixture).get(fixture.staticEndpoint.getId()).getStatus());
            assertEquals(PublishAuditStatus.Status.BUNDLE_SAVED_SUCCESSFULLY,
                    auditAPI.getPublishAuditStatus(fixture.bundle.getId()).getStatus());
        } finally {
            fixture.cleanUp();
        }
    }

    /**
     * Method to test: {@link PushPublisher#process(com.dotcms.publishing.PublishStatus)}
     * Given scenario: the {@code http} endpoint's key is the path of a key file that cannot be read
     * (it does not exist), so no key can be loaded when the bundle is sent
     * Expected result: the endpoint is recorded as FAILED_TO_SENT, so the bundle is retried, and the
     * stored key is left as it was. Before the fix the stored key was overwritten with the invalid
     * token marker and the bundle was removed from the queue, so restoring the file did not help.
     */
    @Test
    public void test_push_unreadableKeyFile_failsWithoutTouchingStoredKey() throws Exception {
        final MixedEndpointEnvironment fixture = PublisherTestUtil.createMixedEndpointEnvironment(
                order(PushPublisher.class), false);
        try {
            final String keyFile = java.nio.file.Files.createTempDirectory("pp-key")
                    .resolve("missing.key").toString();
            final String storedKey = PublicEncryptionFactory.encryptString(keyFile);
            fixture.httpEndpoint.setAuthKey(new StringBuilder(storedKey));
            APILocator.getPublisherEndPointAPI().updateEndPoint(fixture.httpEndpoint);

            new PublisherAPIImpl().publish(fixture.config);

            assertEquals(PublishAuditStatus.Status.FAILED_TO_SENT.getCode(),
                    endpointsOf(fixture).get(fixture.httpEndpoint.getId()).getStatus());
            assertEquals("The stored key must not be replaced", keyFile,
                    PublicEncryptionFactory.decryptString(APILocator.getPublisherEndPointAPI()
                            .findEndPointById(fixture.httpEndpoint.getId()).getAuthKey().toString()));
        } finally {
            fixture.cleanUp();
        }
    }

    private static EndpointDetail detail(final int status, final String info) {
        final EndpointDetail detail = new EndpointDetail();
        detail.setStatus(status);
        detail.setInfo(info);
        return detail;
    }
}
