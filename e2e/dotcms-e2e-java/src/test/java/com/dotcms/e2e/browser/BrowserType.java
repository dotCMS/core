package com.dotcms.e2e.browser;

import java.util.stream.Stream;

/**
 * Enum representing different types of browsers.
 *
 * This enum provides a method to get a browser type from a string value.
 *
 * @author vico
 */
public enum BrowserType {

    CHROMIUM, FIREFOX, WEBKIT;

    /**
     * Returns the BrowserType corresponding to the given string value.
     *
     * @param value the string representation of the browser type
     * @return the BrowserType corresponding to the given value, or null if no match is found
     */
    public static BrowserType fromString(final String value) {
        return Stream.of(values()).filter(element -> element.name().equals(value)).findFirst().orElse(null);
    }

}
