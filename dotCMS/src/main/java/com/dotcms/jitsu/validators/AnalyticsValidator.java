package com.dotcms.jitsu.validators;

import com.dotmarketing.util.json.JSONObject;

/**
 * Interface for validators that check analytics event data.
 * 
 * <p>Implementations of this interface provide specific validation logic
 * for different types of fields in analytics events. Each validator is responsible
 * for checking a specific aspect of a field, such as whether it's required,
 * or if it's of a specific type (string, JSON object, JSON array, etc.).</p>
 * 
 * <p>Validators are typically configured through JSON configuration files and
 * are applied to fields based on the result of the {@link #test(JSONObject)} method.</p>
 */
public interface AnalyticsValidator {

    /**
     * This attribute allows you to specify an additional custom validator that will check the value
     * of a given property.
     */
    String CUSTOM_VALIDATOR_ATTRIBUTE = "custom-validator";

    /**
     * Tests whether this validator should be applied based on the validator configuration.
     * 
     * @param jsonValidatorBody The JSON configuration for this validator
     * @return true if this validator should be applied, false otherwise
     */
    boolean test(final JSONObject jsonValidatorBody);

    /**
     * Validates the given field value according to this validator's rules.
     * 
     * @param fieldValue The value to validate
     * @throws AnalyticsValidationException if validation fails
     */
    void validate(final Object fieldValue) throws AnalyticsValidationException;

    /**
     * Exception thrown when validation fails.
     * 
     * <p>This exception includes a specific error code that indicates the type of validation
     * failure that occurred. The error code can be used to provide more specific error
     * handling or messaging to the user.</p>
     */
    class AnalyticsValidationException extends Exception {
        private final ValidationErrorCode code;

        /**
         * Constructs a new validation exception with the specified message and error code.
         * 
         * @param message The error message
         * @param code The validation error code
         */
        public AnalyticsValidationException(final String message,
                                     final ValidationErrorCode code) {
            super(message);
            this.code = code;
        }

        /**
         * Returns the validation error code associated with this exception.
         * 
         * @return The validation error code
         */
        public ValidationErrorCode getCode(){
            return code;
        }
    }

}
