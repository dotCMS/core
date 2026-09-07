package com.dotcms.system.event.local.type.staticpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;
import com.dotcms.system.event.local.type.pushpublish.EndpointFailureDetail;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Object used to represent an event to be triggered when an endpoint fails during static publishing.
 *
 * <p>Optionally carries per-endpoint {@link EndpointFailureDetail} entries so subscribers can
 * distinguish filesystem and connection failures. Legacy single-arg constructor keeps working;
 * {@link #getEndpointDetails()} returns an empty list when it is used.</p>
 *
 * @author Oscar Arrieta.
 */
public class SingleStaticPublishEndpointFailureEvent extends PublishEvent {

    private final List<EndpointFailureDetail> endpointDetails;

    public SingleStaticPublishEndpointFailureEvent(List<PublishQueueElement> publishQueueElements) {
        this(publishQueueElements, Collections.emptyList());
    }

    public SingleStaticPublishEndpointFailureEvent(List<PublishQueueElement> publishQueueElements,
                                                   List<EndpointFailureDetail> endpointDetails) {
        super(SingleStaticPublishEndpointFailureEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
        this.endpointDetails = endpointDetails != null
                ? List.copyOf(endpointDetails)
                : Collections.emptyList();
    }

    /**
     * Per-endpoint failure information. Never {@code null}; empty when the legacy
     * single-argument constructor was used.
     */
    public List<EndpointFailureDetail> getEndpointDetails() {
        return endpointDetails;
    }

}
