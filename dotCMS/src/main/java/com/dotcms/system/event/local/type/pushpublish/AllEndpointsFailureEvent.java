package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;

import java.util.Date;
import java.util.List;

/**
 * Object used to represent an event to be triggered when all endpoints fail during push publishing
 *
 * @author nollymar
 */
public class AllEndpointsFailureEvent extends PushPublishEvent {

    public AllEndpointsFailureEvent(List<PublishQueueElement> publishQueueElements) {
        this.setName(AllEndpointsFailureEvent.class.getCanonicalName());
        this.setPublishQueueElements(publishQueueElements);
        this.setDate(new Date());
    }

}
