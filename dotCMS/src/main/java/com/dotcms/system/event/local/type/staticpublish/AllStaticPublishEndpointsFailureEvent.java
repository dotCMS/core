package com.dotcms.system.event.local.type.staticpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Object used to represent an event to be triggered when all endpoints fail during static publishing
 *
 * @author Oscar Arrieta.
 */
public class AllStaticPublishEndpointsFailureEvent extends PublishEvent {

    public AllStaticPublishEndpointsFailureEvent(List<PublishQueueElement> publishQueueElements) {
        super(AllStaticPublishEndpointsFailureEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
    }

}
