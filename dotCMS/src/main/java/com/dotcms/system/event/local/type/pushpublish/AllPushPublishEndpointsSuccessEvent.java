package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publishing.PublisherConfig;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.util.Date;

/**
 * Object used to represent an event to be triggered when bundles were sent successfully to all
 * endpoints
 *
 * @author nollymar
 */
public class AllPushPublishEndpointsSuccessEvent extends PublishEvent {

    private PublisherConfig config = null;

    public AllPushPublishEndpointsSuccessEvent(PublisherConfig config) {
        setName(AllPushPublishEndpointsSuccessEvent.class.getCanonicalName());
        setPublishQueueElements(config.getAssets());
        setDate(new Date());
        setConfig(config);
    }

    public PublisherConfig getConfig() {
        return config;
    }

    public void setConfig(PublisherConfig config) {
        this.config = config;
    }
}
