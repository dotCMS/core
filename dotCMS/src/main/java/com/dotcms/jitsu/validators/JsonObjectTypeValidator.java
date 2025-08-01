package com.dotcms.jitsu.validators;

import com.dotmarketing.util.json.JSONObject;

import java.util.Map;

/**
 * Validator that checks if a field value is a JSON object.
 * This validator will fail if the field value is not a Map or JSONObject.
 */
public class JsonObjectTypeValidator implements AnalyticsValidator {

    private static final String TYPE_ATTRIBUTE = "type";
    private static final String JSON_OBJECT_TYPE = "json_object";

    /**
     * Tests if this validator should be applied based on the validator configuration.
     * This validator is applied if the "type" attribute is set to "json object".
     *
     * @param jsonValidatorBody The validator configuration
     * @return true if the validator should be applied, false otherwise
     */
    @Override
    public boolean test(JSONObject jsonValidatorBody) {
        return jsonValidatorBody.has(TYPE_ATTRIBUTE) && 
               JSON_OBJECT_TYPE.equals(jsonValidatorBody.get(TYPE_ATTRIBUTE));
    }

    /**
     * Validates that the field value is a JSON object (Map or JSONObject).
     *
     * @param fieldValue The field value to validate
     * @throws AnalyticsValidationException if the field value is not a JSON object
     */
    @Override
    public void validate(Object fieldValue) throws AnalyticsValidationException {
        if (fieldValue != null && !(fieldValue instanceof Map) && !(fieldValue instanceof JSONObject)) {
            throw new AnalyticsValidationException(
                "Field value is not a JSON object: " + fieldValue, 
                ValidationErrorCode.INVALID_JSON_OBJECT_TYPE
            );
        }
    }
}