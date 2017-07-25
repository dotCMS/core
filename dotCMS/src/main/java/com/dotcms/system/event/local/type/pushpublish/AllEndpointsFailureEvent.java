package com.dotcms.system.event.local.type.pushpublish;

/**
 * Object used to represent an event to be triggered when all endpoints fail during push publishing
 *
 * @author nollymar
 */
public class AllEndpointsFailureEvent extends PushPublishEvent {

    public AllEndpointsFailureEvent(String name) {
        setName(name);
    }

    public AllEndpointsFailureEvent() {
        this(AllEndpointsFailureEvent.class.getCanonicalName());
    }
}
