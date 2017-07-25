package com.dotcms.api.system.event.local.type;

public class AddedToQueueEvent extends PushPublishEvent{

    public AddedToQueueEvent(String name) {
        setName(name);
    }
}
