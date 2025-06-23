package com.dotcms.jitsu.validators;

import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;

/**
 * Validator that checks if a field is required and present in the payload.
 * This validator will fail if the field value is null.
 */
public class RequiredFieldValidator implements AnalyticsValidator {

    private static final String REQUIRED_ATTRIBUTE = "required";

    /**
     * Tests if this validator should be applied based on the validator configuration.
     * This validator is applied if the "required" attribute is set to true.
     *
     * @param jsonValidatorBody The validator configuration
     * @return true if the validator should be applied, false otherwise
     */
    @Override
    public boolean test(JSONObject jsonValidatorBody) {
        return jsonValidatorBody.has(REQUIRED_ATTRIBUTE) && 
               Boolean.TRUE.equals(jsonValidatorBody.get(REQUIRED_ATTRIBUTE));
    }

    /**
     * Validates that the field value is not null.
     *
     * @param fieldValue The field value to validate
     * @throws AnalyticsValidationException if the field value is null
     */
    @Override
    public void validate(Object fieldValue) throws AnalyticsValidationException {
        if ((fieldValue instanceof JSONArray) && ((JSONArray) fieldValue).isEmpty()) {
            throw new AnalyticsValidationException(
                    "Required field is missing or null",
                    ValidationErrorCode.REQUIRED_FIELD_MISSING
            );
        } else {
            if (fieldValue == null || fieldValue.toString().isEmpty()) {
                throw new AnalyticsValidationException(
                        "Required field is missing or null",
                        ValidationErrorCode.REQUIRED_FIELD_MISSING
                );
            }
        }
    }
}