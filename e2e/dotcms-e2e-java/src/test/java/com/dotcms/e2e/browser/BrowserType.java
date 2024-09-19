package com.dotcms.e2e.browser;

import java.util.stream.Stream;

public enum BrowserType {

    CHROMIUM, FIREFOX, WEBKIT;

    public static BrowserType fromString(final String value) {
        return Stream.of(values()).filter(element -> element.name().equals(value)).findFirst().orElse(null);
    }

}
