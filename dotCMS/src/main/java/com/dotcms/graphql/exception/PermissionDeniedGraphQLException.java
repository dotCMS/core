package com.dotcms.graphql.exception;

/**
 * Custom exception for permission denial in GraphQL operations.
 * This exception is used to indicate that a user does not have the necessary permissions
 * to access a specific resource.
 */
public class PermissionDeniedGraphQLException extends CustomGraphQLException {

    private static final String DEFAULT_MESSAGE = "Permission denied: You do not have permission to access this resource.";

    /**
     * Constructs a new PermissionDeniedGraphQLException with a specific message.
     *
     * @param message The detail message to be used.
     */
    public PermissionDeniedGraphQLException(String message) {
        super(message);
    }

    /**
     * Constructs a new PermissionDeniedGraphQLException with a default message.
     */
    public PermissionDeniedGraphQLException() {
        this(DEFAULT_MESSAGE);
    }
}
