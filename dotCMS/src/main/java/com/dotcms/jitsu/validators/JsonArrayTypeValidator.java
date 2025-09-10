package com.dotcms.jitsu.validators;

import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;

import java.util.Collection;

/**
 * Validator that checks if a field value is a JSON array.
 * This validator will fail if the field value is not a Collection or JSONArray.
 */
public class JsonArrayTypeValidator implements AnalyticsValidator {

    private static final String TYPE_ATTRIBUTE = "type";
    private static final String JSON_ARRAY_TYPE = "json_array";

    /**
     * Tests if this validator should be applied based on the validator configuration.
     * This validator is applied if the "type" attribute is set to "json array".
     *
     * @param jsonValidatorBody The validator configuration
     * @return true if the validator should be applied, false otherwise
     */
    @Override
    public boolean test(JSONObject jsonValidatorBody) {
        return jsonValidatorBody.has(TYPE_ATTRIBUTE) && 
               JSON_ARRAY_TYPE.equals(jsonValidatorBody.get(TYPE_ATTRIBUTE));
    }

    /**
     * Validates that the field value is a JSON array (Collection or JSONArray).
     *
     * @param fieldValue The field value to validate
     * @throws AnalyticsValidationException if the field value is not a JSON array
     */
    @Override
    public void validate(Object fieldValue) throws AnalyticsValidationException {
        if (fieldValue != null && !(fieldValue instanceof Collection) && !(fieldValue instanceof JSONArray)) {
            throw new AnalyticsValidationException(
                "Field value is not a JSON array: " + fieldValue, 
                ValidationErrorCode.INVALID_JSON_ARRAY_TYPE
            );
        }
    }
}