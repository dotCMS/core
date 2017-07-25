package com.dotcms.api.system.event.local.type;

public class AllEndpointsFailureEvent extends PushPublishEvent {

    public AllEndpointsFailureEvent(String name) {
        setName(name);
    }
}
