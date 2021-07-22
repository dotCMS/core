package com.dotcms.system.event.local.type.pushpublish.receiver;

import com.dotcms.publishing.PublisherConfig;
import com.dotcms.system.event.local.type.publish.PublishEvent;

import java.time.LocalDateTime;

/**
 * Object used to represent an event to be triggered when bundles were sent successfully to all
 * endpoints on the receiver
 *
 * @author jsanca
 */
public class PushPublishSuccessOnReceiverEvent extends PublishEvent {

    private PublisherConfig config = null;

    public PushPublishSuccessOnReceiverEvent(PublisherConfig config) {
        super(PushPublishSuccessOnReceiverEvent.class.getCanonicalName(), config.getAssets(),
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
