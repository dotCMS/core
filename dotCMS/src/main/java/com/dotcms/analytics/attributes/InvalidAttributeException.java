package com.dotcms.analytics.attributes;

/**
 * Thrown when a custom attribute present in an incoming payload has no mapping for the specified
 * event type.
 */
public class InvalidAttributeException extends RuntimeException {
    /**
     * Creates a new exception indicating the attribute is not supported by the event type.
     *
     * @param eventTypeName the event type name.
     * @param attributeName the custom attribute name that is not mapped.
     */
    public InvalidAttributeException(String eventTypeName, String attributeName) {
        super(String.format("Custom Attribute %s is not supported by %s", attributeName, eventTypeName));
    }
}
