package com.dotcms.analytics.attributes;

/**
 * Thrown when an operation would cause an event type to exceed the maximum number of supported
 * custom attributes.
 */
public class MaxCustomAttributesReachedException extends RuntimeException {
    /**
     * Creates a new exception indicating the limit was reached for a given event type.
     *
     * @param eventTypeName the event type name.
     * @param maxLimitCustomAttributesReached the configured maximum number of custom attributes.
     */
    public MaxCustomAttributesReachedException(final String eventTypeName,
                                               final int maxLimitCustomAttributesReached) {
        super(String.format("Max Number of Custom Attributes Reached (%d) for event %s",
                maxLimitCustomAttributesReached, eventTypeName));
    }
}
