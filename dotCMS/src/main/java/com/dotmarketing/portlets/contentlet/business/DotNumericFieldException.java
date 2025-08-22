package com.dotmarketing.portlets.contentlet.business;

import static com.dotmarketing.util.FieldNameUtils.convertFieldClassName;

import com.dotmarketing.util.importer.ImportLineValidationCodes;
import com.dotmarketing.util.importer.exception.ImportLineError;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DotNumericFieldException extends DotContentletStateException implements
        ImportLineError {

    public static final String INVALID_NUMERIC_FIELD_MESSAGE = "Unable to set string value '%s' as a %s for the field: %s";

    private final String field;
    private final String value;
    private final String fieldType;
    private final String expectedNumericType;
    private final Map<String, String> additionalContext;

    DotNumericFieldException(String message, String field, Object value) {
        super(message);
        this.field = field;
        this.value = value == null ? "" : value.toString();
        this.fieldType = null;
        this.expectedNumericType = null;
        this.additionalContext = new HashMap<>();
    }

    /**
     * Enhanced constructor for Builder pattern
     */
    private DotNumericFieldException(String message, String field, Object value, 
                                   String fieldType, String expectedNumericType, Map<String, String> additionalContext) {
        super(message);
        this.field = field;
        this.value = value == null ? "" : value.toString();
        this.fieldType = fieldType;
        this.expectedNumericType = expectedNumericType;
        this.additionalContext = new HashMap<>(additionalContext);
    }

    @Override
    public Optional<String> getField() {
        return Optional.of(field);
    }

    @Override
    public Optional<String> getValue() {
        return Optional.of(value);
    }

    @Override
    public String getCode() {
        return ImportLineValidationCodes.INVALID_NUMBER_FORMAT.name();
    }

    @Override
    public Optional<Map<String, ?>> getContext() {
        Map<String, String> context = new HashMap<>(additionalContext);
        if (fieldType != null) {
            context.put("fieldType", convertFieldClassName(fieldType));
        }
        if (expectedNumericType != null) {
            context.put("expectedNumericType", expectedNumericType);
        }
        return context.isEmpty() ? Optional.empty() : Optional.of(context);
    }

    /**
     * Builder for creating DotNumericFieldException with enhanced context information
     */
    public static class Builder {
        private final String field;
        private final Object value;
        private final String expectedNumericType;
        private String fieldType;
        private final Map<String, String> additionalContext = new HashMap<>();

        private Builder(String field, Object value, String expectedNumericType) {
            this.field = field;
            this.value = value;
            this.expectedNumericType = expectedNumericType;
        }


        /**
         * Set the type of the field (e.g., "text", "number", etc.)
         */
        public Builder fieldType(String fieldType) {
            this.fieldType = fieldType;
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
         * Build the exception with enhanced context
         */
        public DotNumericFieldException build() {
            String message = String.format(INVALID_NUMERIC_FIELD_MESSAGE, value,
                    expectedNumericType, field);
            return new DotNumericFieldException(message, field, value, fieldType,
                    expectedNumericType, additionalContext);
        }
    }

    /**
     * Create a Builder for Long field exceptions
     */
    public static Builder longFieldBuilder(String field, Object value) {
        return new Builder(field, value, "Long");
    }

    /**
     * Create a Builder for Float field exceptions  
     */
    public static Builder floatFieldBuilder(String field, Object value) {
        return new Builder(field, value, "Float");
    }
}
