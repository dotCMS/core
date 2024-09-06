package com.dotcms.graphql.exception;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.language.SourceLocation;
import java.util.Collections;

import java.util.List;
import java.util.Map;

/**
 * Custom exception class for GraphQL errors.
 * <p>
 * This class extends {@link GraphQLException} and implements {@link GraphQLError},
 * allowing for customized error handling in GraphQL applications.
 * </p>
 * <p>
 * By implementing {@link GraphQLError}, this exception can provide additional
 * context or metadata about the error, such as locations, extensions, and error types.
 * </p>
 */
public class CustomGraphQLException extends RuntimeException implements GraphQLError {

    /**
     * Constructs a new CustomGraphQLException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception.
     */
    public CustomGraphQLException(String message) {
        super(message);
    }

    /**
     * Provides additional metadata about the error.
     * <p>
     * This implementation returns an empty map, but subclasses can override this
     * method to include custom extensions that provide more information about the error.
     * </p>
     *
     * @return an empty map by default, which can be overridden by subclasses.
     */
    @Override
    public Map<String, Object> getExtensions() {
        return Collections.emptyMap();
    }

    /**
     * Returns the list of source locations related to the error.
     * <p>
     * This implementation returns an empty list, but subclasses can override this
     * method to include the specific locations in the GraphQL query where the error occurred.
     * </p>
     *
     * @return an empty list by default, which can be overridden by subclasses.
     */
    @Override
    public List<SourceLocation> getLocations() {
        return Collections.emptyList();
    }

    /**
     * Returns the type of the error.
     * <p>
     * This implementation returns {@code null}, but subclasses can override this
     * method to provide a specific {@link ErrorType} for the error.
     * </p>
     *
     * @return {@code null} by default, which can be overridden by subclasses.
     */
    @Override
    public ErrorType getErrorType() {
        return null;
    }
}