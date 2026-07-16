package com.dotcms.graphql.exception;

import graphql.ErrorType;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception representing an invalid language error in GraphQL.
 * <p>
 * This exception is used when a requested language is not valid or not available.
 * It extends {@link CustomGraphQLException} and provides specific error type
 * and extension data related to language validation errors.
 * </p>
 */
public class InvalidLanguageException extends CustomGraphQLException {

    private final Long languageId;
    private final String requestedOperation;

    /**
     * Constructs a new InvalidLanguageException with a generic message.
     */
    public InvalidLanguageException() {
        this("Invalid or unsupported language requested", null, null);
    }

    /**
     * Constructs a new InvalidLanguageException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception.
     */
    public InvalidLanguageException(String message) {
        this(message, null, null);
    }

    /**
     * Constructs a new InvalidLanguageException with information about the invalid language.
     *
     * @param languageId the ID of the invalid or unsupported language.
     */
    public InvalidLanguageException(Long languageId) {
        this("Invalid or unsupported language ID: " + languageId, languageId, null);
    }

    /**
     * Constructs a new InvalidLanguageException with information about the language and operation.
     *
     * @param languageId the ID of the invalid or unsupported language.
     * @param requestedOperation the operation that was being attempted with the invalid language.
     */
    public InvalidLanguageException(Long languageId, String requestedOperation) {
        this("Invalid language ID: " + languageId + " for operation: " + requestedOperation,
                languageId, requestedOperation);
    }

    /**
     * Constructs a new InvalidLanguageException with a custom message and language information.
     *
     * @param message the detail message explaining the reason for the exception.
     * @param languageId the ID of the invalid or unsupported language.
     * @param requestedOperation the operation that was being attempted with the invalid language.
     */
    public InvalidLanguageException(String message, Long languageId, String requestedOperation) {
        super(message);
        this.languageId = languageId;
        this.requestedOperation = requestedOperation;
    }

    /**
     * Returns the type of the error.
     *
     * @return {@link ErrorType#ValidationError} as this is a validation error.
     */
    @Override
    public ErrorType getErrorType() {
        return ErrorType.ValidationError;
    }

    /**
     * Provides additional metadata about the error.
     *
     * @return a map containing error code, HTTP status code, and information about the invalid language.
     */
    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("code", "INVALID_LANGUAGE");
        extensions.put("status", 400); // Bad Request status code

        if (languageId != null) {
            extensions.put("languageId", languageId);
        }

        if (requestedOperation != null) {
            extensions.put("requestedOperation", requestedOperation);
        }

        return extensions;
    }
}