package com.dotmarketing.util.importer.exception;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.importer.model.AbstractValidationMessage.ValidationMessageType;
import com.dotmarketing.util.importer.model.ValidationMessage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Base exception class for validation-related errors during the content import process. This class
 * extends DotRuntimeException and provides additional fields and functionality to support the creation
 * of ValidationMessage objects. It serves as the parent class for more specific validation exceptions
 * like HeaderValidationException and ImportLineException.
 *
 * <p>This exception includes:
 * <ul>
 *   <li>Validation message type (ERROR)</li>
 *   <li>Error code for specific validation failures</li>
 *   <li>Line number where the error occurred</li>
 *   <li>Field name associated with the error</li>
 *   <li>Invalid value that caused the error</li>
 *   <li>Additional context information</li>
 * </ul>
 *
 * @see DotRuntimeException
 * @see HeaderValidationException
 * @see ImportLineException
 */
public class ValidationMessageException extends DotRuntimeException {

    private final ValidationMessageType type;
    private final String code;
    private final Integer lineNumber;
    private final String field;
    private final String invalidValue;
    private final Map<String, Object> context;

    protected ValidationMessageException(Builder<?> builder) {
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
     * @return The type of validation message (ERROR)
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

    public ValidationMessage toValidationMessage() {
        return ValidationMessage.builder()
                .type(type)
                .message(getMessage())
                .code(getCode())
                .lineNumber(getLineNumber())
                .field(getField())
                .invalidValue(getInvalidValue())
                .context(context)
                .build();
    }

    @SuppressWarnings("unchecked")
    protected abstract static class Builder<T extends Builder<T>> {

        private ValidationMessageType type = ValidationMessageType.ERROR;
        private String message;
        private String code;
        private Integer lineNumber;
        private String field;
        private String invalidValue;
        private Map<String, Object> context;

        public T message(String message) {
            this.message = message;
            return (T) this;
        }

        public T code(String code) {
            this.code = code;
            return (T) this;
        }

        public T lineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
            return (T) this;
        }

        public T field(String field) {
            this.field = field;
            return (T) this;
        }

        public T invalidValue(String invalidValue) {
            this.invalidValue = invalidValue;
            return (T) this;
        }

        public T context(Map<String, Object> context) {
            this.context = context;
            return (T) this;
        }

        protected void validate() {
            if (message == null) {
                throw new IllegalStateException("Message is required");
            }
        }

        public abstract ValidationMessageException build();
    }

}
