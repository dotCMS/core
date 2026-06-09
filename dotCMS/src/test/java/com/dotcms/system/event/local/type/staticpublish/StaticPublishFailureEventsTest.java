package com.dotcms.system.event.local.type.staticpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.pushpublish.EndpointFailureDetail;
import com.dotcms.system.event.local.type.pushpublish.FailureCategory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Backward-compatibility and enrichment tests for the static-publish failure event classes.
 *
 * <p>Mirrors {@code PushPublishFailureEventsTest} for the static-publishing variants.</p>
 */
public class StaticPublishFailureEventsTest {

    private static EndpointFailureDetail sampleDetail(final FailureCategory category) {
        return EndpointFailureDetail.builder()
                .endpointId("ep-1")
                .endpointName("server-a")
                .environmentId("env-1")
                .failureCategory(category)
                .message("error")
                .build();
    }

    // ── AllStaticPublishEndpointsFailureEvent ──────────────────────────────────

    @Test
    public void allFailureEvent_legacyConstructor_returnsEmptyEndpointDetails() {
        final AllStaticPublishEndpointsFailureEvent event =
                new AllStaticPublishEndpointsFailureEvent(Collections.<PublishQueueElement>emptyList());
        assertNotNull(event.getEndpointDetails());
        assertTrue(event.getEndpointDetails().isEmpty());
    }

    @Test
    public void allFailureEvent_newConstructor_carriesEndpointDetails() {
        final List<EndpointFailureDetail> details = List.of(
                sampleDetail(FailureCategory.FILESYSTEM_ERROR),
                sampleDetail(FailureCategory.CONNECTION_ERROR));
        final AllStaticPublishEndpointsFailureEvent event =
                new AllStaticPublishEndpointsFailureEvent(Collections.<PublishQueueElement>emptyList(), details);
        assertEquals(2, event.getEndpointDetails().size());
        assertEquals(FailureCategory.FILESYSTEM_ERROR, event.getEndpointDetails().get(0).getFailureCategory());
        assertEquals(FailureCategory.CONNECTION_ERROR, event.getEndpointDetails().get(1).getFailureCategory());
    }

    @Test
    public void allFailureEvent_endpointDetailsList_isUnmodifiable() {
        final AllStaticPublishEndpointsFailureEvent event = new AllStaticPublishEndpointsFailureEvent(
                Collections.<PublishQueueElement>emptyList(),
                List.of(sampleDetail(FailureCategory.FILESYSTEM_ERROR)));
        try {
            event.getEndpointDetails().clear();
            fail("Expected UnsupportedOperationException — endpoint-details list must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void allFailureEvent_endpointDetailsList_isDefensivelyCopied() {
        final List<EndpointFailureDetail> source = new ArrayList<>();
        source.add(sampleDetail(FailureCategory.FILESYSTEM_ERROR));
        final AllStaticPublishEndpointsFailureEvent event =
                new AllStaticPublishEndpointsFailureEvent(Collections.<PublishQueueElement>emptyList(), source);

        source.add(sampleDetail(FailureCategory.CONNECTION_ERROR));
        source.clear();

        assertEquals(1, event.getEndpointDetails().size());
        assertEquals(FailureCategory.FILESYSTEM_ERROR, event.getEndpointDetails().get(0).getFailureCategory());
    }

    @Test
    public void allFailureEvent_nullDetails_areCoercedToEmpty() {
        final AllStaticPublishEndpointsFailureEvent event =
                new AllStaticPublishEndpointsFailureEvent(Collections.<PublishQueueElement>emptyList(), null);
        assertNotNull(event.getEndpointDetails());
        assertTrue(event.getEndpointDetails().isEmpty());
    }

    // ── SingleStaticPublishEndpointFailureEvent ────────────────────────────────

    @Test
    public void singleFailureEvent_legacyConstructor_returnsEmptyEndpointDetails() {
        final SingleStaticPublishEndpointFailureEvent event =
                new SingleStaticPublishEndpointFailureEvent(Collections.<PublishQueueElement>emptyList());
        assertNotNull(event.getEndpointDetails());
        assertTrue(event.getEndpointDetails().isEmpty());
    }

    @Test
    public void singleFailureEvent_newConstructor_carriesEndpointDetails() {
        final List<EndpointFailureDetail> details = List.of(sampleDetail(FailureCategory.CONNECTION_ERROR));
        final SingleStaticPublishEndpointFailureEvent event =
                new SingleStaticPublishEndpointFailureEvent(
                        Collections.<PublishQueueElement>emptyList(), details);
        assertEquals(1, event.getEndpointDetails().size());
        assertEquals(FailureCategory.CONNECTION_ERROR, event.getEndpointDetails().get(0).getFailureCategory());
    }

    @Test
    public void singleFailureEvent_nullDetails_areCoercedToEmpty() {
        final SingleStaticPublishEndpointFailureEvent event =
                new SingleStaticPublishEndpointFailureEvent(
                        Collections.<PublishQueueElement>emptyList(), null);
        assertNotNull(event.getEndpointDetails());
        assertTrue(event.getEndpointDetails().isEmpty());
    }

    // ── FailureCategory static-publishing variants ─────────────────────────────

    @Test
    public void filesystemError_isNotRetryable() {
        assertFalse(FailureCategory.FILESYSTEM_ERROR.isRetryable());
    }

    @Test
    public void connectionError_isRetryable() {
        assertTrue(FailureCategory.CONNECTION_ERROR.isRetryable());
    }
}
