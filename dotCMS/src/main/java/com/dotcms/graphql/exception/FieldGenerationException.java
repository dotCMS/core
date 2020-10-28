package com.dotcms.graphql.exception;

import com.dotmarketing.exception.DotRuntimeException;
import graphql.schema.GraphQLFieldDefinition;

/**
 * Runtime Exception thrown when a {@link GraphQLFieldDefinition} can't be generated
 */
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
