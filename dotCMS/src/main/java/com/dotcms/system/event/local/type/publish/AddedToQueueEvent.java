package com.dotcms.system.event.local.type.publish;

import com.dotcms.publisher.business.PublishQueueElement;

import java.util.Date;
import java.util.List;

/**
 * Object used to represent an event to be triggered when a new asset is queued to publish
 *
 * @author nollymar
 */
public class AddedToQueueEvent extends PublishEvent {

    public AddedToQueueEvent(List<PublishQueueElement> publishQueueElements){
        this.setName(AddedToQueueEvent.class.getCanonicalName());
        this.setPublishQueueElements(publishQueueElements);
        this.setDate(new Date());
    }
}
