package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Object used to represent an event to be triggered when all endpoints fail during push publishing.
 *
 * <p>In addition to the assets in the bundle, the event optionally carries a list of
 * {@link EndpointFailureDetail} entries — one per failed endpoint — so that subscribers can
 * distinguish authentication, authorization, server, network and bundle failures, capture HTTP
 * status codes, and decide whether to retry. Subscribers compiled against the original constructor
 * keep working: when that constructor is used {@link #getEndpointDetails()} returns an empty list.</p>
 *
 * @author nollymar
 */
public class AllPushPublishEndpointsFailureEvent extends PublishEvent {

    private final List<EndpointFailureDetail> endpointDetails;

    public AllPushPublishEndpointsFailureEvent(List<PublishQueueElement> publishQueueElements) {
        this(publishQueueElements, Collections.emptyList());
    }

    public AllPushPublishEndpointsFailureEvent(List<PublishQueueElement> publishQueueElements,
                                               List<EndpointFailureDetail> endpointDetails) {
        super(AllPushPublishEndpointsFailureEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
        this.endpointDetails = endpointDetails != null
                ? Collections.unmodifiableList(endpointDetails)
                : Collections.emptyList();
    }

    /**
     * Per-endpoint failure information. Never {@code null}; empty when the event was published
     * via the legacy single-argument constructor.
     */
    public List<EndpointFailureDetail> getEndpointDetails() {
        return endpointDetails;
    }

}
