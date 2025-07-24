package com.dotcms.analytics.attributes;

public class InvalidAttributeException extends RuntimeException {
    public InvalidAttributeException(String eventTypeName, String attributeName) {
        super(String.format("Custom Attribute %s is not support by %s", attributeName, eventTypeName));
    }
}
