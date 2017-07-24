package com.dotcms.api.system.event.local;

import java.io.Serializable;

/**
 * This is a wrapper event for orphan (events with not subscribed associated)
 * @author jsanca
 */
public class OrphanEvent implements Serializable {

    private final Object orphanEvent;

    public OrphanEvent(Object orphanEvent) {
        this.orphanEvent = orphanEvent;
    }

    public Object getOrphanEvent() {
        return orphanEvent;
    }

    @Override
    public String toString() {
        return "OrphanEvent{" +
                "orphanEvent=" +
                    ((null != orphanEvent)?orphanEvent.toString(): "null") +
                '}';
    }
} // E:O:F:OrphanEvent.
