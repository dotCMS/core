package com.dotcms.system.event.local.type.pushpublish.receiver;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Object used to represent an event to be triggered when all endpoints fail during push publishing on the receiver
 *
 * @author jsanca
 */
public class PushPublishFailureOnReceiverEvent extends PublishEvent {

    private final Throwable e;

    public PushPublishFailureOnReceiverEvent(final List<PublishQueueElement> publishQueueElements) {
        super(PushPublishFailureOnReceiverEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
        e = null;
    }

    public PushPublishFailureOnReceiverEvent(final List<PublishQueueElement> publishQueueElements, final Throwable e) {
        super(PushPublishFailureOnReceiverEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
        this.e = e;
    }

}