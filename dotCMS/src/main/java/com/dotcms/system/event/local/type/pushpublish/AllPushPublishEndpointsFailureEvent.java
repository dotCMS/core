package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.util.Date;
import java.util.List;

/**
 * Object used to represent an event to be triggered when all endpoints fail during push publishing
 *
 * @author nollymar
 */
public class AllPushPublishEndpointsFailureEvent extends PublishEvent {

    public AllPushPublishEndpointsFailureEvent(List<PublishQueueElement> publishQueueElements) {
        this.setName(AllPushPublishEndpointsFailureEvent.class.getCanonicalName());
        this.setPublishQueueElements(publishQueueElements);
        this.setDate(new Date());
    }

}
