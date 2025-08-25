package com.dotmarketing.portlets.contentlet.business;

import static com.dotmarketing.util.FieldNameUtils.convertFieldClassName;

import com.dotmarketing.util.FieldNameUtils;
import com.dotmarketing.util.importer.ImportLineValidationCodes;
import com.dotmarketing.util.importer.exception.ImportLineError;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DotDateFieldException extends DotContentletStateException implements
        ImportLineError {

    public static final String INVALID_DATE_CONVERSION_MESSAGE = "Unable to convert string to date %s, field: %s";
    public static final String INVALID_DATE_TYPE_MESSAGE = "Date fields must either be of type String or Date, field: %s";
    
    // Field-type-specific messages
    public static final String INVALID_DATE_FIELD_CONVERSION_MESSAGE = "Unable to convert string '%s' to date, field: %s";
    public static final String INVALID_DATETIME_FIELD_CONVERSION_MESSAGE = "Unable to convert string '%s' to date-time, field: %s";
    public static final String INVALID_TIME_FIELD_CONVERSION_MESSAGE = "Unable to convert string '%s' to time, field: %s";

    private final String field;
    private final String value;
    private final String fieldType;
    private final String expectedFormat;
    private final String[] acceptedFormats;
    private final Map<String, String> additionalContext;

    DotDateFieldException(String message, String field, Object value) {
        super(message);
        this.field = field;
        this.value = value == null ? "" : value.toString();
        this.fieldType = null;
        this.expectedFormat = null;
        this.acceptedFormats = null;
        this.additionalContext = new HashMap<>();
    }

    /**
     * Enhanced constructor for Builder pattern
     */
    private DotDateFieldException(String message, String field, Object value, 
                                 String fieldType, String expectedFormat, String[] acceptedFormats, 
                                 Map<String, String> additionalContext) {
        super(message);
        this.field = field;
        this.value = value == null ? "" : value.toString();
        this.fieldType = fieldType;
        this.expectedFormat = expectedFormat;
        this.acceptedFormats = acceptedFormats;
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
        return ImportLineValidationCodes.INVALID_DATE_FORMAT.name();
    }

    @Override
    public Optional<Map<String, ?>> getContext() {
        Map<String, String> context = new HashMap<>(additionalContext);
        if (fieldType != null) {
            context.put("fieldType", convertFieldClassName(fieldType));
        }
        if (expectedFormat != null) {
            context.put("expectedFormat", expectedFormat);
        }
        if (acceptedFormats != null && acceptedFormats.length > 0) {
            context.put("acceptedFormats", String.join(", ", acceptedFormats));
        }
        return context.isEmpty() ? Optional.empty() : Optional.of(context);
    }

    /**
     * Builder for creating DotDateFieldException with enhanced context information
     */
    public static class Builder {
        private final String field;
        private final Object value;
        private String fieldType;
        private String expectedFormat;
        private String[] acceptedFormats;
        private final Map<String, String> additionalContext = new HashMap<>();
        private String messageTemplate = INVALID_DATE_CONVERSION_MESSAGE;

        private Builder(String field, Object value) {
            this.field = field;
            this.value = value;
        }


        /**
         * Set the type of the field (e.g., "date", "datetime", etc.)
         */
        public Builder fieldType(String fieldType) {
            this.fieldType = fieldType;
            return this;
        }

        /**
         * Set the expected date format
         */
        public Builder expectedFormat(String expectedFormat) {
            this.expectedFormat = expectedFormat;
            return this;
        }

        /**
         * Set the accepted date formats
         */
        public Builder acceptedFormats(String... acceptedFormats) {
            this.acceptedFormats = acceptedFormats;
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
         * Set this as an invalid type error (not a conversion error)
         */
        public Builder invalidType() {
            this.messageTemplate = INVALID_DATE_TYPE_MESSAGE;
            return this;
        }

        /**
         * Build the exception with enhanced context
         */
        public DotDateFieldException build() {
            String message;
            if (messageTemplate.equals(INVALID_DATE_TYPE_MESSAGE)) {
                message = String.format(messageTemplate, field);
            } else {
                // Use field type specific messages when available
                String specificMessageTemplate = getFieldTypeSpecificMessage();
                message = String.format(specificMessageTemplate, value, field);
            }
            return new DotDateFieldException(message, field, value, fieldType, 
                                           expectedFormat, acceptedFormats, additionalContext);
        }
        
        /**
         * Get the appropriate error message template based on field type
         */
        private String getFieldTypeSpecificMessage() {
            if (fieldType != null) {
                switch (fieldType.toLowerCase()) {
                    case "date":
                        return INVALID_DATE_FIELD_CONVERSION_MESSAGE;
                    case "datetime":
                    case "date_time":
                        return INVALID_DATETIME_FIELD_CONVERSION_MESSAGE;
                    case "time":
                        return INVALID_TIME_FIELD_CONVERSION_MESSAGE;
                }
            }
            // Fallback to the default generic message
            return messageTemplate;
        }
    }

    /**
     * Create a Builder for date conversion errors
     */
    public static Builder conversionErrorBuilder(String field, Object value) {
        return new Builder(field, value);
    }

    /**
     * Create a Builder for invalid date type errors
     */
    public static Builder invalidTypeBuilder(String field, Object value) {
        return new Builder(field, value).invalidType();
    }

    /**
     * Legacy method for basic date conversion errors (backward compatibility)
     */
    public static DotDateFieldException newDateConversionException(String field, Object value) {
        return new DotDateFieldException(
                String.format(INVALID_DATE_CONVERSION_MESSAGE, value, field),
                field, value
        );
    }

    /**
     * Legacy method for invalid date type errors (backward compatibility)
     */
    public static DotDateFieldException newInvalidDateTypeException(String field, Object value) {
        return new DotDateFieldException(
                String.format(INVALID_DATE_TYPE_MESSAGE, field),
                field, value
        );
    }
}