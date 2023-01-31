package com.dotcms.analytics.metrics;

import java.util.Arrays;

public enum EventType {
    PAGE_VIEW("pageview");

    private String name;

    EventType(final String name) {
        this.name = name;
    }

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
