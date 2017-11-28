package com.dotcms.system.event.local.type.publish;

import com.dotcms.publisher.business.PublishQueueElement;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Object used to represent an event to be triggered when a new asset is queued to publish
 *
 * @author nollymar
 */
public class AddedToQueueEvent extends PublishEvent {

    public AddedToQueueEvent(List<PublishQueueElement> publishQueueElements){

        super(AddedToQueueEvent.class.getCanonicalName(), publishQueueElements,
                LocalDateTime.now());
    }
}
