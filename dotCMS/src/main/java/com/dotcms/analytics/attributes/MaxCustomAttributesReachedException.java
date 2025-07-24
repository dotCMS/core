package com.dotcms.analytics.attributes;

public class MaxCustomAttributesReachedException extends RuntimeException {
    public MaxCustomAttributesReachedException(final String eventTypeName,
                                               final int maxLimitCustomAttributesReached) {
        super(String.format("Max Number of Custom Attributes Reached (%d) for event %s",
                maxLimitCustomAttributesReached, eventTypeName));
    }
}
