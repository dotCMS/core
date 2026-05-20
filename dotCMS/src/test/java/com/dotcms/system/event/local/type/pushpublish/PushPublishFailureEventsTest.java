package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Backward-compatibility tests for the failure-event classes: existing single-argument
 * constructors must keep working and {@link AllPushPublishEndpointsFailureEvent#getEndpointDetails()}
 * /{@link SinglePushPublishEndpointFailureEvent#getEndpointDetails()} must return an empty,
 * never-null list when those constructors are used.
 */
public class PushPublishFailureEventsTest {

    private static EndpointFailureDetail sampleDetail(final FailureCategory category) {
        return EndpointFailureDetail.builder()
                .endpointId("endpoint-1")
                .endpointName("server-a")
                .environmentId("env-1")
                .failureCategory(category)
                .httpStatusCode(category == FailureCategory.SERVER_ERROR ? 503 : null)
                .message("Boom")
                .build();
    }

    @Test
    public void allFailureEvent_legacyConstructor_returnsEmptyEndpointDetails() {
        final AllPushPublishEndpointsFailureEvent event =
                new AllPushPublishEndpointsFailureEvent(Collections.<PublishQueueElement>emptyList());
        assertNotNull(event.getEndpointDetails());
        assertTrue(event.getEndpointDetails().isEmpty());
    }

    @Test
    public void allFailureEvent_newConstructor_carriesEndpointDetails() {
        final List<EndpointFailureDetail> details = List.of(
                sampleDetail(FailureCategory.AUTHENTICATION),
                sampleDetail(FailureCategory.SERVER_ERROR));
        final AllPushPublishEndpointsFailureEvent event = new AllPushPublishEndpointsFailureEvent(
                Collections.<PublishQueueElement>emptyList(), details);
        assertEquals(2, event.getEndpointDetails().size());
        assertEquals(FailureCategory.AUTHENTICATION,
                event.getEndpointDetails().get(0).getFailureCategory());
        assertEquals(FailureCategory.SERVER_ERROR,
                event.getEndpointDetails().get(1).getFailureCategory());
    }

    @Test
    public void allFailureEvent_endpointDetailsList_isUnmodifiable() {
        final AllPushPublishEndpointsFailureEvent event = new AllPushPublishEndpointsFailureEvent(
                Collections.<PublishQueueElement>emptyList(),
                List.of(sampleDetail(FailureCategory.NETWORK_ERROR)));
        try {
            event.getEndpointDetails().clear();
            fail("Expected UnsupportedOperationException — endpoint-details list must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void allFailureEvent_nullDetails_areCoercedToEmpty() {
        final AllPushPublishEndpointsFailureEvent event = new AllPushPublishEndpointsFailureEvent(
                Collections.<PublishQueueElement>emptyList(), null);
        assertNotNull(event.getEndpointDetails());
        assertTrue(event.getEndpointDetails().isEmpty());
    }

    @Test
    public void singleFailureEvent_legacyConstructor_returnsEmptyEndpointDetails() {
        final SinglePushPublishEndpointFailureEvent event =
                new SinglePushPublishEndpointFailureEvent(Collections.<PublishQueueElement>emptyList());
        assertNotNull(event.getEndpointDetails());
        assertTrue(event.getEndpointDetails().isEmpty());
    }

    @Test
    public void singleFailureEvent_newConstructor_carriesEndpointDetails() {
        final List<EndpointFailureDetail> details =
                List.of(sampleDetail(FailureCategory.AUTHORIZATION));
        final SinglePushPublishEndpointFailureEvent event =
                new SinglePushPublishEndpointFailureEvent(
                        Collections.<PublishQueueElement>emptyList(), details);
        assertEquals(1, event.getEndpointDetails().size());
        assertEquals(FailureCategory.AUTHORIZATION,
                event.getEndpointDetails().get(0).getFailureCategory());
    }
}
