package com.dotcms.system.event.local.type.pushpublish.receiver;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Object used to represent an event to be triggered when the push publishing process finishes
 *
 * @author nollymar
 */
public class PushPublishEndOnReceiverEvent extends PublishEvent {

    public PushPublishEndOnReceiverEvent(List<PublishQueueElement> publishQueueElements) {
        super(PushPublishEndOnReceiverEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
    }
    
}
