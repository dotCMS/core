package com.dotcms.graphql.exception;

import com.dotmarketing.exception.DotRuntimeException;

public class FieldGenerationException extends DotRuntimeException {

    public FieldGenerationException(String message) {
        super(message);
    }

    public FieldGenerationException(Throwable cause) {
        super(cause);
    }

    public FieldGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
