package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publishing.PublisherConfig;

import java.util.Date;

/**
 * Object used to represent an event to be triggered when bundles were sent successfully to all
 * endpoints
 *
 * @author nollymar
 */
public class AllEndpointsSuccessEvent extends PushPublishEvent {

    private PublisherConfig config = null;

    public AllEndpointsSuccessEvent(PublisherConfig config) {
        setName(AllEndpointsSuccessEvent.class.getCanonicalName());
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
