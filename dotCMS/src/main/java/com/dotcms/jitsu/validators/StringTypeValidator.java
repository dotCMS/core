package com.dotcms.jitsu.validators;

import com.dotmarketing.util.json.JSONObject;

/**
 * Validator that checks if a field value is a string.
 * This validator will fail if the field value is not a string.
 */
public class StringTypeValidator implements AnalyticsValidator {

    private static final String TYPE_ATTRIBUTE = "type";
    private static final String STRING_TYPE = "string";

    /**
     * Tests if this validator should be applied based on the validator configuration.
     * This validator is applied if the "type" attribute is set to "string".
     *
     * @param jsonValidatorBody The validator configuration
     * @return true if the validator should be applied, false otherwise
     */
    @Override
    public boolean test(JSONObject jsonValidatorBody) {
        return jsonValidatorBody.has(TYPE_ATTRIBUTE) && 
               STRING_TYPE.equals(jsonValidatorBody.get(TYPE_ATTRIBUTE));
    }

    /**
     * Validates that the field value is a string.
     *
     * @param fieldValue The field value to validate
     * @throws AnalyticsValidationException if the field value is not a string
     */
    @Override
    public void validate(Object fieldValue) throws AnalyticsValidationException {
        if (fieldValue != null && !(fieldValue instanceof String)) {
            throw new AnalyticsValidationException(
                    String.format("Field value is not a String: %s", fieldValue),
                    ValidationErrorCode.INVALID_STRING_TYPE
            );
        }
    }
}