package com.dotcms.system.event.local.type.pushpublish;

/**
 * Object used to represent an event to be triggered when the push publishing process finishes
 *
 * @author nollymar
 */
public class PushPublishEndEvent extends PushPublishEvent {

    public PushPublishEndEvent(String name) {
        setName(name);
    }

    public PushPublishEndEvent() {
        this(PushPublishEndEvent.class.getCanonicalName());
    }
}
