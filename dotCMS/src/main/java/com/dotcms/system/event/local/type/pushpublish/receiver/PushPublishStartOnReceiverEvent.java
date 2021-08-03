package com.dotcms.system.event.local.type.pushpublish.receiver;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Object used to represent an event to be triggered when the push publishing process starts on the receiver
 *
 * @author jsanca
 */
public class PushPublishStartOnReceiverEvent extends PublishEvent {

    public PushPublishStartOnReceiverEvent(final List<PublishQueueElement> publishQueueElements) {
        super(PushPublishStartOnReceiverEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
    }

}
