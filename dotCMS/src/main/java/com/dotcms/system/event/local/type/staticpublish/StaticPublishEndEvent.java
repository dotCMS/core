package com.dotcms.system.event.local.type.staticpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Object used to represent an event to be triggered when the static publishing process finishes
 *
 * @author Oscar Arrieta
 */
public class StaticPublishEndEvent extends PublishEvent {

    public StaticPublishEndEvent(List<PublishQueueElement> publishQueueElements) {

        super(StaticPublishEndEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
    }
}
