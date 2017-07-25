package com.dotcms.api.system.event.local.type;

/**
 * Object used to represent an event to be triggered when bundles were sent successfully to all endpoints
 *
 * @author nollymar
 */
public class AllEndpointsSuccessEvent extends PushPublishEvent {

    public AllEndpointsSuccessEvent(String name) {
        setName(name);
    }

    public AllEndpointsSuccessEvent() {
        this(AllEndpointsSuccessEvent.class.getCanonicalName());
    }
}
