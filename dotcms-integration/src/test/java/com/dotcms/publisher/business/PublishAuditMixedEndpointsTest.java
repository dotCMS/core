package com.dotcms.publisher.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.LicenseTestUtil;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.business.PublisherTestUtil.MixedEndpointEnvironment;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publishing.PublisherAPIImpl;
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

    private static EndpointDetail detail(final int status, final String info) {
        final EndpointDetail detail = new EndpointDetail();
        detail.setStatus(status);
        detail.setInfo(info);
        return detail;
    }
}
