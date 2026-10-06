package com.dotcms.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Event notifying that a system table key has been updated or removed.
 * <p>It is fired locally and also published cluster wide wrapped in a
 * {@link com.dotcms.api.system.event.SystemEventType#CLUSTER_WIDE_EVENT} payload, which means it is
 * serialized to JSON and rebuilt on the receiving node by
 * {@code com.dotcms.util.jackson.PayloadDeserializer}. The {@link JsonCreator} constructor is what
 * makes that round trip possible; without it the receiving node cannot rebuild the event and the
 * whole batch of polled events is discarded.</p>
 * @author jsanca
 */
public class SystemTableUpdatedKeyEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String key;

    @JsonCreator
    public SystemTableUpdatedKeyEvent(@JsonProperty("key") final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
