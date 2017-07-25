package com.dotcms.api.system.event.local.type;

/**
 * Object used to represent an event to be triggered among a push publishing process, ie: assets added to queue,
 * push publishing starts, push publishing ends, etc
 *
 * @author nollymar
 */
public class PushPublishEvent {
    private String name = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
