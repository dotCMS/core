package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.UnitTestBase;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotmarketing.business.APILocator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Demonstrates and verifies what a customer plugin subscriber for the push-publishing failure
 * events would look like. The test wires a {@link FailureSubscriber} POJO into the real
 * {@link LocalSystemEventsAPI}, fires the two failure events synchronously with hand-built
 * {@link EndpointFailureDetail} payloads, and asserts the subscriber can inspect failure
 * category, HTTP status, audit status and retryability through the new event payload.
 *
 * <p>This is also the canonical example we point integrators at: register a POJO with
 * {@code @Subscriber}-annotated methods, then react to failures using
 * {@link EndpointFailureDetail#getFailureCategory()} and
 * {@link EndpointFailureDetail#isRetryable()}.</p>
 */
public class PushPublishFailureEventSubscriberTest extends UnitTestBase {

    private LocalSystemEventsAPI localSystemEventsAPI;
    private FailureSubscriber subscriber;

    @Before
    public void setUp() {
        this.localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        this.subscriber = new FailureSubscriber();
        this.localSystemEventsAPI.subscribe(this.subscriber);
    }

    @After
    public void tearDown() {
        this.localSystemEventsAPI.unsubscribe(this.subscriber);
    }

    @Test
    public void allEndpointsFailureEvent_isDeliveredAndCarriesPerEndpointDetails() {
        final List<EndpointFailureDetail> details = List.of(
                detail("endpoint-prod-1", "prod-1", "env-prod", FailureCategory.AUTHENTICATION,
                        PublishAuditStatus.Status.INVALID_TOKEN, 401, "Invalid token"),
                detail("endpoint-prod-2", "prod-2", "env-prod", FailureCategory.SERVER_ERROR,
                        PublishAuditStatus.Status.FAILED_TO_SENT, 503, "Service unavailable"),
                detail("endpoint-prod-3", "prod-3", "env-prod", FailureCategory.NETWORK_ERROR,
                        PublishAuditStatus.Status.FAILED_TO_SENT, null, "Connection refused"));

        localSystemEventsAPI.notify(
                new AllPushPublishEndpointsFailureEvent(
                        Collections.<PublishQueueElement>emptyList(), details));

        assertEquals(1, subscriber.allFailureCount);
        assertEquals(0, subscriber.singleFailureCount);
        assertEquals(3, subscriber.lastAllFailure.getEndpointDetails().size());

        // A subscriber can group failures by category — this is the canonical use case.
        final Map<FailureCategory, Integer> byCategory = subscriber.byCategory;
        assertEquals(Integer.valueOf(1), byCategory.get(FailureCategory.AUTHENTICATION));
        assertEquals(Integer.valueOf(1), byCategory.get(FailureCategory.SERVER_ERROR));
        assertEquals(Integer.valueOf(1), byCategory.get(FailureCategory.NETWORK_ERROR));

        // And inspect HTTP status / retryable hint per endpoint.
        assertEquals(Integer.valueOf(401),
                subscriber.lastAllFailure.getEndpointDetails().get(0).getHttpStatusCode());
        assertFalse(subscriber.lastAllFailure.getEndpointDetails().get(0).isRetryable());
        assertTrue(subscriber.lastAllFailure.getEndpointDetails().get(1).isRetryable());
        assertTrue(subscriber.lastAllFailure.getEndpointDetails().get(2).isRetryable());
    }

    @Test
    public void singleEndpointFailureEvent_isDeliveredAndOnlyFailedEndpointsAreReported() {
        final List<EndpointFailureDetail> details = List.of(
                detail("endpoint-staging-1", "staging-1", "env-staging", FailureCategory.AUTHORIZATION,
                        PublishAuditStatus.Status.LICENSE_REQUIRED, 403, "License required"));

        localSystemEventsAPI.notify(
                new SinglePushPublishEndpointFailureEvent(
                        Collections.<PublishQueueElement>emptyList(), details));

        assertEquals(1, subscriber.singleFailureCount);
        assertEquals(0, subscriber.allFailureCount);

        final EndpointFailureDetail received =
                subscriber.lastSingleFailure.getEndpointDetails().get(0);
        assertEquals(FailureCategory.AUTHORIZATION, received.getFailureCategory());
        assertEquals(PublishAuditStatus.Status.LICENSE_REQUIRED, received.getAuditStatus());
        assertEquals(Integer.valueOf(403), received.getHttpStatusCode());
        assertFalse(received.isRetryable());
        assertEquals("License required", received.getMessage());
    }

    @Test
    public void legacyConstructor_stillDeliversEvent_withEmptyDetails() {
        // Backward-compat check: subscribers must still receive events built via the legacy
        // single-argument constructor, and getEndpointDetails() must return an empty list.
        localSystemEventsAPI.notify(new AllPushPublishEndpointsFailureEvent(
                Collections.<PublishQueueElement>emptyList()));

        assertEquals(1, subscriber.allFailureCount);
        assertNotNull(subscriber.lastAllFailure.getEndpointDetails());
        assertTrue(subscriber.lastAllFailure.getEndpointDetails().isEmpty());
    }

    private static EndpointFailureDetail detail(final String endpointId,
                                                final String endpointName,
                                                final String environmentId,
                                                final FailureCategory category,
                                                final PublishAuditStatus.Status auditStatus,
                                                final Integer httpStatus,
                                                final String message) {
        return EndpointFailureDetail.builder()
                .endpointId(endpointId)
                .endpointName(endpointName)
                .environmentId(environmentId)
                .environmentName(environmentId)
                .failureCategory(category)
                .auditStatus(auditStatus)
                .httpStatusCode(httpStatus)
                .message(message)
                .build();
    }

    /**
     * Reference subscriber a customer plugin could ship — registers {@code @Subscriber}
     * methods for the two failure events and reacts to them.
     */
    public static class FailureSubscriber {

        int allFailureCount = 0;
        int singleFailureCount = 0;
        AllPushPublishEndpointsFailureEvent lastAllFailure;
        SinglePushPublishEndpointFailureEvent lastSingleFailure;
        final Map<FailureCategory, Integer> byCategory = new EnumMap<>(FailureCategory.class);
        final List<EndpointFailureDetail> retryablesSeen = new ArrayList<>();

        @Subscriber
        public void onAllFailed(final AllPushPublishEndpointsFailureEvent event) {
            this.allFailureCount++;
            this.lastAllFailure = event;
            consume(event.getEndpointDetails());
        }

        @Subscriber
        public void onSomeFailed(final SinglePushPublishEndpointFailureEvent event) {
            this.singleFailureCount++;
            this.lastSingleFailure = event;
            consume(event.getEndpointDetails());
        }

        private void consume(final List<EndpointFailureDetail> details) {
            for (final EndpointFailureDetail d : details) {
                byCategory.merge(d.getFailureCategory(), 1, Integer::sum);
                if (d.isRetryable()) {
                    retryablesSeen.add(d);
                }
            }
        }
    }
}
