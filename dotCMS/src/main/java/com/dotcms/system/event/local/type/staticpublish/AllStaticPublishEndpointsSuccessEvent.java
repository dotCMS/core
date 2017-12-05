package com.dotcms.system.event.local.type.staticpublish;

import com.dotcms.publishing.PublisherConfig;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;

/**
 * Object used to represent an event to be triggered when static bundles were sent successfully to all
 * endpoints
 *
 * @author Oscar Arrieta.
 */
public class AllStaticPublishEndpointsSuccessEvent extends PublishEvent {

    private PublisherConfig config = null;

    public AllStaticPublishEndpointsSuccessEvent(PublisherConfig config) {

        super(AllStaticPublishEndpointsSuccessEvent.class.getCanonicalName(), config.getAssets(),
                LocalDateTime.now());
        setConfig(config);
    }

    public PublisherConfig getConfig() {
        return config;
    }

    public void setConfig(PublisherConfig config) {
        this.config = config;
    }

}
