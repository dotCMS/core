package com.dotcms.system.event.local.type.pushpublish;

/**
 * Object used to represent an event to be triggered when an endpoint fails during push publishing
 *
 * @author nollymar
 */
public class SingleEndpointFailureEvent extends PushPublishEvent {

    public SingleEndpointFailureEvent(String name) {
        setName(name);
    }

    public SingleEndpointFailureEvent() {
        this(SingleEndpointFailureEvent.class.getCanonicalName());
    }
}
