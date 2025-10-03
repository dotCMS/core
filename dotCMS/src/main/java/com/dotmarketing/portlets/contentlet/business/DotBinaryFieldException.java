package com.dotmarketing.portlets.contentlet.business;

import static com.dotmarketing.util.FieldNameUtils.convertFieldClassName;

import com.dotmarketing.util.FieldNameUtils;
import com.dotmarketing.util.importer.ImportLineValidationCodes;
import com.dotmarketing.util.importer.exception.ImportLineError;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DotBinaryFieldException extends DotContentletStateException implements
        ImportLineError {

    /**
     * Enum for binary field error types
     */
    public enum ErrorType {
        INVALID_FILE("INVALID_FILE"),
        IO_ERROR("IO_ERROR"),
        INVALID_BINARY_DATA("INVALID_BINARY_DATA");

        private final String code;

        ErrorType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static final String INVALID_TEMP_FILE_MESSAGE = "Invalid Temp File provided, for the field: %s";
    public static final String UNABLE_TO_SET_BINARY_MESSAGE = "Unable to set binary file Object: %s";

    private final String field;
    private final String value;
    private final String fieldType;
    private final ErrorType errorType;
    private final String expectedFormat;
    private final Map<String, String> additionalContext;

    DotBinaryFieldException(String message, String field, Object value) {
        super(message);
        this.field = field;
        this.value = value == null ? "" : value.toString();
        this.fieldType = null;
        this.errorType = ErrorType.INVALID_BINARY_DATA;
        this.expectedFormat = null;
        this.additionalContext = new HashMap<>();
    }

    /**
     * Enhanced constructor for Builder pattern
     */
    private DotBinaryFieldException(String message, String field, Object value, 
                                   String fieldType, ErrorType errorType, String expectedFormat,
                                   Map<String, String> additionalContext, Exception cause) {
        super(message, cause);
        this.field = field;
        this.value = value == null ? "" : value.toString();
        this.fieldType = fieldType;
        this.errorType = errorType;
        this.expectedFormat = expectedFormat;
        this.additionalContext = new HashMap<>(additionalContext);
    }

    @Override
    public Optional<String> getField() {
        return Optional.ofNullable(field);
    }

    @Override
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public String getCode() {
        switch (errorType) {
            case INVALID_FILE:
                return ImportLineValidationCodes.FILE_NOT_FOUND.name();
            case IO_ERROR:
                return ImportLineValidationCodes.INVALID_BINARY_URL.name();
            default:
                return ImportLineValidationCodes.INVALID_FIELD_TYPE.name();
        }
    }

    @Override
    public Optional<Map<String, ?>> getContext() {
        Map<String, String> context = new HashMap<>(additionalContext);
        if (fieldType != null) {
            context.put("fieldType", convertFieldClassName(fieldType));
        }
        if (errorType != null) {
            context.put("errorType", errorType.getCode());
        }
        if (expectedFormat != null) {
            context.put("expectedFormat", expectedFormat);
        }
        return context.isEmpty() ? Optional.empty() : Optional.of(context);
    }

    /**
     * Builder for creating DotBinaryFieldException with enhanced context information
     */
    public static class Builder {
        private final String field;
        private final Object value;
        private final ErrorType errorType;
        private String fieldType;
        private String expectedFormat;
        private final Map<String, String> additionalContext = new HashMap<>();
        private final String messageTemplate;
        private Exception cause;

        private Builder(String field, Object value, ErrorType errorType, String messageTemplate) {
            this.field = field;
            this.value = value;
            this.errorType = errorType;
            this.messageTemplate = messageTemplate;
        }


        /**
         * Set the type of the field (e.g., "binary", "file", etc.)
         */
        public Builder fieldType(String fieldType) {
            this.fieldType = fieldType;
            return this;
        }

        /**
         * Set the expected format
         */
        public Builder expectedFormat(String expectedFormat) {
            this.expectedFormat = expectedFormat;
            return this;
        }

        /**
         * Add additional context information
         */
        public Builder addContext(String key, String value) {
            this.additionalContext.put(key, value);
            return this;
        }

        /**
         * Set the underlying cause of the exception
         */
        public Builder cause(Exception cause) {
            this.cause = cause;
            return this;
        }

        /**
         * Build the exception with enhanced context
         */
        public DotBinaryFieldException build() {
            String message;
            if (messageTemplate.equals(INVALID_TEMP_FILE_MESSAGE)) {
                message = String.format(messageTemplate, field);
            } else {
                message = String.format(messageTemplate, 
                    cause != null ? cause.getMessage() : "Unknown error");
            }
            return new DotBinaryFieldException(message, field, value, fieldType, 
                                             errorType, expectedFormat, additionalContext, cause);
        }
    }

    /**
     * Create a Builder for invalid temp file errors
     */
    public static Builder invalidTempFileBuilder(String field, Object value) {
        return new Builder(field, value, ErrorType.INVALID_FILE, INVALID_TEMP_FILE_MESSAGE);
    }

    /**
     * Create a Builder for IO errors during binary file operations
     */
    public static Builder ioErrorBuilder(String field, Object value) {
        return new Builder(field, value, ErrorType.IO_ERROR, UNABLE_TO_SET_BINARY_MESSAGE);
    }

    /**
     * Legacy method for invalid temp file errors (backward compatibility)
     */
    public static DotBinaryFieldException newInvalidTempFileException(String field, Object value) {
        return new DotBinaryFieldException(
                String.format(INVALID_TEMP_FILE_MESSAGE, field),
                field, value
        );
    }

    /**
     * Legacy method for IO errors (backward compatibility)
     */
    public static DotBinaryFieldException newIoException(String field, Object value, Exception cause) {
        return new Builder(field, value, ErrorType.IO_ERROR, UNABLE_TO_SET_BINARY_MESSAGE)
                .cause(cause)
                .build();
    }
}