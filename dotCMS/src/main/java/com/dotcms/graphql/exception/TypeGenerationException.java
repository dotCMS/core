package com.dotcms.graphql.exception;

import com.dotmarketing.exception.DotRuntimeException;
import graphql.schema.GraphQLType;


/**
 * Runtime Exception thrown when a {@link GraphQLType} can't be generated
 */
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
