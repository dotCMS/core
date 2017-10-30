package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.util.Date;
import java.util.List;

/**
 * Object used to represent an event to be triggered when an endpoint fails during push publishing
 *
 * @author nollymar
 */
public class SinglePushPublishEndpointFailureEvent extends PublishEvent {

    public SinglePushPublishEndpointFailureEvent(List<PublishQueueElement> publishQueueElements) {
        this.setName(SinglePushPublishEndpointFailureEvent.class.getCanonicalName());
        this.setPublishQueueElements(publishQueueElements);
        this.setDate(new Date());
    }

}
