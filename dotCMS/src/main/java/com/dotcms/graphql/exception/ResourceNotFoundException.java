package com.dotcms.graphql.exception;

import graphql.ErrorType;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception representing a 404 Not Found error in GraphQL.
 * <p>
 * This exception is used when a requested resource cannot be found.
 * It extends {@link CustomGraphQLException} and provides specific error type
 * and extension data related to 404 errors.
 * </p>
 */
public class ResourceNotFoundException extends CustomGraphQLException {

    private final String resourceType;
    private final String resourceId;

    /**
     * Constructs a new ResourceNotFoundException with a generic message.
     */
    public ResourceNotFoundException() {
        this("Resource not found", null, null);
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception.
     */
    public ResourceNotFoundException(String message) {
        this(message, null, null);
    }

    /**
     * Constructs a new ResourceNotFoundException with information about the resource.
     *
     * @param resourceType the type of resource that was not found (e.g., "ContentType", "Page").
     * @param resourceId the identifier of the resource that was not found.
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        this("Resource not found: " + resourceType + " with ID " + resourceId, resourceType, resourceId);
    }

    /**
     * Constructs a new ResourceNotFoundException with a custom message and resource information.
     *
     * @param message the detail message explaining the reason for the exception.
     * @param resourceType the type of resource that was not found.
     * @param resourceId the identifier of the resource that was not found.
     */
    public ResourceNotFoundException(String message, String resourceType, String resourceId) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Returns the type of the error.
     *
     * @return {@link ErrorType#DataFetchingException} as this is a data fetching error.
     */
    @Override
    public ErrorType getErrorType() {
        return ErrorType.DataFetchingException;
    }

    /**
     * Provides additional metadata about the error.
     *
     * @return a map containing HTTP status code and additional resource information if available.
     */
    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("code", "NOT_FOUND");
        extensions.put("status", 404);

        if (resourceType != null) {
            extensions.put("resourceType", resourceType);
        }

        if (resourceId != null) {
            extensions.put("resourceId", resourceId);
        }

        return extensions;
    }
}