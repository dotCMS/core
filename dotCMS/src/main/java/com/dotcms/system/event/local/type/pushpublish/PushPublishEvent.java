package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishQueueElement;
import java.util.Date;
import java.util.List;

/**
 * Object used to represent an event to be triggered among a push publishing process, ie: assets
 * added to queue, push publishing starts, push publishing ends, etc
 *
 * @author nollymar
 */
public class PushPublishEvent {

    private String name = null;
    private List<PublishQueueElement> publishQueueElements = null;
    private Date date;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PublishQueueElement> getPublishQueueElements() {
        return publishQueueElements;
    }

    public void setPublishQueueElements(
            List<PublishQueueElement> publishQueueElements) {
        this.publishQueueElements = publishQueueElements;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
