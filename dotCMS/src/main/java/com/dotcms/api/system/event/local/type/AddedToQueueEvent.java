package com.dotcms.api.system.event.local.type;

/**
 * Object used to represent an event to be triggered when a new asset is queued to publish
 *
 * @author nollymar
 */
public class AddedToQueueEvent extends PushPublishEvent {

    public AddedToQueueEvent(String name) {
        setName(name);
    }

    public AddedToQueueEvent(){
        this(AddedToQueueEvent.class.getCanonicalName());
    }
}
