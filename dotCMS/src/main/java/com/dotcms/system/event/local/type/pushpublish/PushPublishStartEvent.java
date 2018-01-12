package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Object used to represent an event to be triggered when the push publishing process starts
 *
 * @author nollymar
 */
public class PushPublishStartEvent extends PublishEvent {

    public PushPublishStartEvent(List<PublishQueueElement> publishQueueElements) {
        super(PushPublishStartEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
    }

}
