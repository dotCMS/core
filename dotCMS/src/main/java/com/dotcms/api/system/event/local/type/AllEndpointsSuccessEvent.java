package com.dotcms.api.system.event.local.type;

public class AllEndpointsSuccessEvent extends PushPublishEvent {

    public AllEndpointsSuccessEvent(String name) {
        setName(name);
    }
}
