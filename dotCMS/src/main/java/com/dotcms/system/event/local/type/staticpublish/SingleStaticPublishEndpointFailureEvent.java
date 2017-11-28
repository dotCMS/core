package com.dotcms.system.event.local.type.staticpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Object used to represent an event to be triggered when an endpoint fails during static publishing
 *
 * @author Oscar Arrieta.
 */
public class SingleStaticPublishEndpointFailureEvent extends PublishEvent {

    public SingleStaticPublishEndpointFailureEvent(List<PublishQueueElement> publishQueueElements) {

        super(SingleStaticPublishEndpointFailureEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
    }

}
