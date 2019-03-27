package com.dotcms.system.event.local.type.publish;

import com.dotcms.publisher.business.PublishQueueElement;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Object used to represent an event to be triggered among a push publishing process, ie: assets
 * added to queue, push publishing starts, push publishing ends, etc
 *
 * @author nollymar
 */
public class PublishEvent {

    private String name = null;
    private List<PublishQueueElement> publishQueueElements = null;
    private LocalDateTime date;

    public PublishEvent(String name,
            List<PublishQueueElement> publishQueueElements, LocalDateTime date) {
        this.name = name;
        if (publishQueueElements != null) {
            this.publishQueueElements = Collections.unmodifiableList(publishQueueElements);
        }
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PublishQueueElement> getPublishQueueElements() {
        return publishQueueElements!=null?Collections.unmodifiableList(publishQueueElements):null;
    }

    public void setPublishQueueElements(List<PublishQueueElement> publishQueueElements) {
        if (publishQueueElements != null) {
            this.publishQueueElements = Collections.unmodifiableList(publishQueueElements);
        }else{
            this.publishQueueElements = null;
        }
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
