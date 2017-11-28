package com.dotcms.system.event.local.type.staticpublish;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.system.event.local.type.publish.PublishEvent;
import java.time.LocalDateTime;

/**
 *  Object used to represent an event to be triggered when an endpoint successes during static publishing
 *
 * @author nollymar
 */
public class SingleStaticPublishEndpointSuccessEvent extends PublishEvent {

    private PublisherConfig config = null;
    private PublishingEndPoint endpoint;

    public SingleStaticPublishEndpointSuccessEvent(PublisherConfig config, PublishingEndPoint endpoint) {
        super(SingleStaticPublishEndpointSuccessEvent.class.getCanonicalName(), config.getAssets(),
                LocalDateTime.now());
        setConfig(config);
        setEndpoint(endpoint);
    }

    public PublisherConfig getConfig() {
        return config;
    }

    public void setConfig(PublisherConfig config) {
        this.config = config;
    }

    public PublishingEndPoint getEndpoint(){
        return endpoint;
    }

    public void setEndpoint(PublishingEndPoint endpoint){
        this.endpoint = endpoint;
    }

}
