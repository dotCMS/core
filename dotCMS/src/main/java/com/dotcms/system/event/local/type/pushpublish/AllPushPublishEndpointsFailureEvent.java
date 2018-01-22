package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Object used to represent an event to be triggered when all endpoints fail during push publishing
 *
 * @author nollymar
 */
public class AllPushPublishEndpointsFailureEvent extends PublishEvent {

    public AllPushPublishEndpointsFailureEvent(List<PublishQueueElement> publishQueueElements) {
        super(AllPushPublishEndpointsFailureEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
    }

}
