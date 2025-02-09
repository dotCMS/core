package com.dotmarketing.util.importer.exception;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.importer.model.AbstractValidationMessage.ValidationMessageType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Exception class for handling errors during import line processing. Extends DotRuntimeException
 * and provides additional fields to support creation of ValidationMessage objects.
 */
public class ImportLineException extends DotRuntimeException {

    private final ValidationMessageType type;
    private final String code;
    private final Integer lineNumber;
    private final String field;
    private final String invalidValue;
    private final Map<String, Object> context;

    private ImportLineException(Builder builder) {
        super(builder.message);
        this.type = builder.type;
        this.code = builder.code;
        this.lineNumber = builder.lineNumber;
        this.field = builder.field;
        this.invalidValue = builder.invalidValue;
        this.context = Collections.unmodifiableMap(
                builder.context != null ? builder.context : new HashMap<>());
    }

    /**
     * @return The type of validation message (ERROR, WARNING, INFO)
     */
    public ValidationMessageType getType() {
        return type;
    }

    /**
     * @return The validation code, may be null
     */
    public Optional<String> getCode() {
        return Optional.ofNullable(code);
    }

    /**
     * @return The line number where the error occurred
     */
    public Optional<Integer> getLineNumber() {
        return Optional.ofNullable(lineNumber);
    }

    /**
     * @return The field associated with the error, may be null
     */
    public Optional<String> getField() {
        return Optional.ofNullable(field);
    }

    /**
     * @return The invalid value that caused the error, may be null
     */
    public Optional<String> getInvalidValue() {
        return Optional.ofNullable(invalidValue);
    }

    /**
     * @return Additional context information about the error
     */
    public Map<String, Object> getContext() {
        return context;
    }

    public static class Builder {

        private ValidationMessageType type = ValidationMessageType.ERROR;
        private String message;
        private String code;
        private Integer lineNumber;
        private String field;
        private String invalidValue;
        private Map<String, Object> context;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder lineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }

        public Builder invalidValue(String invalidValue) {
            this.invalidValue = invalidValue;
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context = context;
            return this;
        }

        public ImportLineException build() {
            if (message == null) {
                throw new IllegalStateException("Message is required");
            }
            return new ImportLineException(this);
        }
    }

    /**
     * Factory method to create a builder instance.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a ValidationMessage instance from this exception.
     *
     * @return A ValidationMessage object containing the exception data
     */
    public com.dotmarketing.util.importer.model.ValidationMessage toValidationMessage() {
        return com.dotmarketing.util.importer.model.ValidationMessage.builder()
                .type(type)
                .message(getMessage())
                .code(getCode())
                .lineNumber(getLineNumber())
                .field(getField())
                .invalidValue(getInvalidValue())
                .context(context)
                .build();
    }
}
