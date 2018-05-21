package com.dotcms.util;

/**
 * Throw when a error occur in a json serializer o deserializer
 */
public class JsonProcessingRuntimeException extends RuntimeException {

    public JsonProcessingRuntimeException (final Throwable rootCause) {
        super(rootCause);
    }
}
