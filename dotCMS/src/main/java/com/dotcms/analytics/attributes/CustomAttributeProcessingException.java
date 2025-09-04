package com.dotcms.analytics.attributes;

/**
 * Exception thrown when a custom attribute query or result cannot be processed due to
 * insufficient or ambiguous information. For example, this can occur when an event type
 * cannot be uniquely determined from a Cube.js query's filters, preventing resolution of
 * custom attribute mappings.
 */
public class CustomAttributeProcessingException extends Exception {

    /**
     * Creates a new exception with a descriptive message.
     *
     * @param message details about the processing error.
     */
    public CustomAttributeProcessingException(String message) {
        super(message);
    }
}
