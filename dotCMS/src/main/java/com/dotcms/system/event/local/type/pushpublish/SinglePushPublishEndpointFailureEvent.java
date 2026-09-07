package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Object used to represent an event to be triggered when at least one — but not all — endpoints
 * fail during push publishing.
 *
 * <p>The event optionally carries a list of {@link EndpointFailureDetail} entries describing the
 * failed endpoints (successful endpoints are omitted) so subscribers can distinguish authentication,
 * authorization, server, network and bundle failures, capture HTTP status codes, and decide whether
 * to retry. Subscribers compiled against the original constructor keep working: when that
 * constructor is used {@link #getEndpointDetails()} returns an empty list.</p>
 *
 * @author nollymar
 */
public class SinglePushPublishEndpointFailureEvent extends PublishEvent {

    private final List<EndpointFailureDetail> endpointDetails;

    public SinglePushPublishEndpointFailureEvent(List<PublishQueueElement> publishQueueElements) {
        this(publishQueueElements, Collections.emptyList());
    }

    public SinglePushPublishEndpointFailureEvent(List<PublishQueueElement> publishQueueElements,
                                                 List<EndpointFailureDetail> endpointDetails) {

        super(SinglePushPublishEndpointFailureEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
        this.setName(SinglePushPublishEndpointFailureEvent.class.getCanonicalName());
        this.setPublishQueueElements(publishQueueElements);
        this.endpointDetails = endpointDetails != null
                ? List.copyOf(endpointDetails)
                : Collections.emptyList();
    }

    /**
     * Per-endpoint failure information for the endpoints that failed in this run. Never
     * {@code null}; empty when the event was published via the legacy single-argument
     * constructor.
     */
    public List<EndpointFailureDetail> getEndpointDetails() {
        return endpointDetails;
    }

}
