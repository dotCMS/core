package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.EventType;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a Event that can be trigger in a {@link com.dotcms.experiments.business.result.BrowserSession}
 * These events are storage into a Jitsu Server.
 */
public class Event {

    private Map<String, Object> eventAttributes;
    private EventType type;

    public Event(final Map<String, Object> eventAttributes, final EventType eventType) {
        this.eventAttributes = eventAttributes;
        this.type = eventType;
    }

    public Optional<Object> get(final String name) {
        final String key = "Events." + name;
        return Optional.ofNullable(eventAttributes.get(key));
    }

    public EventType getType() {
        return type;
    }
}
