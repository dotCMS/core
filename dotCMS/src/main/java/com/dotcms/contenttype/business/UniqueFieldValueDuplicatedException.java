package com.dotcms.contenttype.business;

import static com.dotmarketing.util.FieldNameUtils.convertFieldClassName;

import com.dotmarketing.util.FieldNameUtils;
import com.dotmarketing.util.importer.ImportLineValidationCodes;
import com.dotmarketing.util.importer.exception.ImportLineError;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Throw if try to insert a duplicated register in unique_fields table
 */
public class UniqueFieldValueDuplicatedException extends Exception implements ImportLineError {


    private final List<String> contentletsIDS;
    private final String field;
    private final String value;
    private final String fieldType;
    private final String contentType;
    private final Map<String, String> additionalContext;

    public UniqueFieldValueDuplicatedException(String message) {
        this(message, null);
    }

    public UniqueFieldValueDuplicatedException(String message, List<String> contentletsIDS) {
        super(message);
        this.contentletsIDS = contentletsIDS;
        this.field = null;
        this.value = null;
        this.fieldType = null;
        this.contentType = null;
        this.additionalContext = new HashMap<>();
    }

    /**
     * Enhanced constructor for Builder pattern
     */
    private UniqueFieldValueDuplicatedException(String message, List<String> contentletsIDS, 
                                               String field, Object value, 
                                               String fieldType, String contentType,
                                               Map<String, String> additionalContext) {
        super(message);
        this.contentletsIDS = contentletsIDS;
        this.field = field;
        this.value = value == null ? "" : value.toString();
        this.fieldType = fieldType;
        this.contentType = contentType;
        this.additionalContext = new HashMap<>(additionalContext);
    }

    public List<String> getContentlets() {
        return contentletsIDS;
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
        return ImportLineValidationCodes.DUPLICATE_UNIQUE_VALUE.name();
    }

    @Override
    public Optional<Map<String, ?>> getContext() {
        Map<String, String> context = new HashMap<>(additionalContext);
        if (fieldType != null) {
            context.put("fieldType", convertFieldClassName(fieldType));
        }
        if (contentType != null) {
            context.put("contentType", contentType);
        }
        if (contentletsIDS != null && !contentletsIDS.isEmpty()) {
            context.put("duplicatedContentlets", String.join(", ", contentletsIDS));
        }
        return context.isEmpty() ? Optional.empty() : Optional.of(context);
    }

    /**
     * Builder for creating UniqueFieldValueDuplicatedException with enhanced context information
     */
    public static class Builder {
        private final String message;
        private final String field;
        private final Object value;
        private final String contentType;
        private String fieldType;
        private List<String> contentletsIDS;
        private final Map<String, String> additionalContext = new HashMap<>();

        private Builder(String message, String field, Object value, String contentType) {
            this.message = message;
            this.field = field;
            this.value = value;
            this.contentType = contentType;
        }

        /**
         * Set the type of the field (e.g., "text", "textarea", etc.)
         */
        public Builder fieldType(String fieldType) {
            this.fieldType = fieldType;
            return this;
        }

        /**
         * Set the list of contentlet IDs that have the duplicated value
         */
        public Builder contentletIds(List<String> contentletIds) {
            this.contentletsIDS = contentletIds;
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
        public UniqueFieldValueDuplicatedException build() {
            return new UniqueFieldValueDuplicatedException(message, contentletsIDS, field, value, 
                                                          fieldType, contentType, additionalContext);
        }
    }

    /**
     * Create a Builder with a pre-computed message
     */
    public static Builder builder(String message, String field, Object value, String contentType) {
        return new Builder(message, field, value, contentType);
    }

}
