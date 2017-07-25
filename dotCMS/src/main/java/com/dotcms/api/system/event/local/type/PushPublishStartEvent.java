package com.dotcms.api.system.event.local.type;

/**
 * Object used to represent an event to be triggered when the push publishing process starts
 *
 * @author nollymar
 */
public class PushPublishStartEvent extends PushPublishEvent {

    public PushPublishStartEvent(String name) {
        setName(name);
    }

    public PushPublishStartEvent() {
        this(PushPublishStartEvent.class.getCanonicalName());
    }
}
