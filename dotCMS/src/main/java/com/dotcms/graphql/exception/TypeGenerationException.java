package com.dotcms.graphql.exception;

import com.dotmarketing.exception.DotRuntimeException;

public class TypeGenerationException extends DotRuntimeException {

    public TypeGenerationException(String message) {
        super(message);
    }

    public TypeGenerationException(Throwable cause) {
        super(cause);
    }

    public TypeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
