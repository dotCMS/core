package com.dotcms.api.system.event.local;


/**
 * This is a wrapper event for orphan (events with not subscribed associated)
 * @author jsanca
 */
public class OrphanEvent {

    private final Object event;

    public OrphanEvent(Object event) {
        this.event = event;
    }

    public Object getEvent() {
        return event;
    }

    @Override
    public String toString() {
        return "OrphanEvent{" +
                "event=" +
                    ((null != event)? event.toString(): "null") +
                '}';
    }
} // E:O:F:OrphanEvent.
