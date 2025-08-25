package com.dotcms.analytics.attributes;

/**
 * Thrown when a translation is requested for an event type that has no stored custom attribute
 * mapping.
 */
public class MissingCustomAttributeMatchException extends RuntimeException {
    /**
     * Creates a new exception indicating that the mapping is missing for the event type.
     *
     * @param eventTypeName the event type name with no mapping.
     */
    public MissingCustomAttributeMatchException(String eventTypeName) {
        super(String.format("No custom attributes match the specified event '%s'", eventTypeName));
    }
}
