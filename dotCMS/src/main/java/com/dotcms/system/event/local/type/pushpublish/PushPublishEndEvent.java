package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;

import java.util.Date;
import java.util.List;

/**
 * Object used to represent an event to be triggered when the push publishing process finishes
 *
 * @author nollymar
 */
public class PushPublishEndEvent extends PushPublishEvent {

    public PushPublishEndEvent(List<PublishQueueElement> publishQueueElements) {
        this.setName(PushPublishEndEvent.class.getCanonicalName());
        this.setPublishQueueElements(publishQueueElements);
        this.setDate(new Date());
    }
    
}
