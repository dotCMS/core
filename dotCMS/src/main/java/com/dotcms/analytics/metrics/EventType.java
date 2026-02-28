package com.dotcms.analytics.metrics;

import java.util.Arrays;

/**
 * Represent a Type of Event that can be trigger in a {@link com.dotcms.experiments.business.result.BrowserSession}
 * These events are storage into a Jitsu Server.
 */
public enum EventType {
    PAGE_VIEW("pageview"),
    CONTENT_IMPRESSION("content_impression"),
    CONTENT_CLICK("content_click"),
    CONVERSION("conversion");

    private String name;

    EventType(final String name) {
        this.name = name;
    }

    /**
     * Get the EventType object from the event name.
     * @param eventName
     * @return
     */
    public static EventType get(final String eventName) {
         return Arrays.stream(EventType.values())
                .filter(eventType -> eventType.getName().equals(eventName))
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Event %s does not exists", eventName)));
    }

    public String getName() {
        return name;
    }
}
