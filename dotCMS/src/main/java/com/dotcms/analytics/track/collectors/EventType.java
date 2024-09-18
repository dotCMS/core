package com.dotcms.analytics.track.collectors;

public enum EventType {
    VANITY_REQUEST("VANITY_REQUEST"),
    FILE_REQUEST("FILE_REQUEST"),
    PAGE_REQUEST("PAGE_REQUEST"),

    URL_MAP("URL_MAP");

    private final String type;
    private EventType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }
}
