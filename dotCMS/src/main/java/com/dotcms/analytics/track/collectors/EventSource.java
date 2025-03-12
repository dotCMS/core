package com.dotcms.analytics.track.collectors;

/**
 * Enum for the Event Sources
 * @author jsanca
 */
public enum EventSource {

    DOT_CMS("DOT_CMS"),
    REST_API("REST_API"),
    WORKFLOW("WORKFLOW"),
    RULE("RULE"),
    DOT_CLI("DOT_CLI");

    private final String name;

    EventSource(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
