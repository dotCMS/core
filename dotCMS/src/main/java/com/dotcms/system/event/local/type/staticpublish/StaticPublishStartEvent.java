package com.dotcms.system.event.local.type.staticpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Object used to represent an event to be triggered when the static publishing process starts
 *
 * @author Oscar Arrieta.
 */
public class StaticPublishStartEvent extends PublishEvent {

    public StaticPublishStartEvent(List<PublishQueueElement> publishQueueElements) {

        super(StaticPublishStartEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
    }

}
