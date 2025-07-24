package com.dotcms.analytics.attributes;

public class MissingCustomAttributeMatchException extends RuntimeException {
    public MissingCustomAttributeMatchException(String eventTypeName) {
        super(String.format("The event %s haven't any match", eventTypeName));
    }
}
