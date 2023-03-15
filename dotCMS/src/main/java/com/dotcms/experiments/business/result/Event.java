package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.EventType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a Event that can be trigger in a {@link com.dotcms.experiments.business.result.BrowserSession}
 * These events are storage into a Jitsu Server.
 */
public class Event {

    private final static DateTimeFormatter  PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.n")
            .withZone(ZoneId.systemDefault());
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

    public Optional<String> getVariant() {
        return  Optional.of (get("variant")
                    .map(variantObject -> variantObject.toString())
                    .orElseThrow());
    }

    public Optional<Instant> getDate() {
        return get("utcTime")
                .map(dateAsObject -> dateAsObject.toString())
                .map(dateAsString -> PARSER.parse(dateAsString))
                .map(temporalAccessor -> Instant.from(temporalAccessor));
    }
}
