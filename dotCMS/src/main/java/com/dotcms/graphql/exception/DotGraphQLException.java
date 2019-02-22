package com.dotcms.graphql.exception;

import java.util.List;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.language.SourceLocation;

public class DotGraphQLException extends GraphQLException implements GraphQLError {

    private ErrorType errorType;

    public DotGraphQLException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return null;
    }

    @Override
    public ErrorType getErrorType() {
        return errorType;
    }
}
