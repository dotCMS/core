package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;

import java.util.Date;
import java.util.List;

/**
 * Object used to represent an event to be triggered when an endpoint fails during push publishing
 *
 * @author nollymar
 */
public class SingleEndpointFailureEvent extends PushPublishEvent {

    public SingleEndpointFailureEvent(List<PublishQueueElement> publishQueueElements) {
        this.setName(SingleEndpointFailureEvent.class.getCanonicalName());
        this.setPublishQueueElements(publishQueueElements);
        this.setDate(new Date());
    }

}
