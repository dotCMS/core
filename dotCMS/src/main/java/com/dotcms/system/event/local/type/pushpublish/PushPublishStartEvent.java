package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;

import java.util.Date;
import java.util.List;

/**
 * Object used to represent an event to be triggered when the push publishing process starts
 *
 * @author nollymar
 */
public class PushPublishStartEvent extends PushPublishEvent {

    public PushPublishStartEvent(List<PublishQueueElement> publishQueueElements) {
        this.setName(PushPublishStartEvent.class.getCanonicalName());
        this.setPublishQueueElements(publishQueueElements);
        this.setDate(new Date());
    }

}
