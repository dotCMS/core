package com.dotcms.api.system.event.local.type;

public class SingleEndpointFailureEvent extends PushPublishEvent {

    public SingleEndpointFailureEvent(String name) {
        setName(name);
    }
}
