package com.dotcms.jitsu.validators;

import com.dotmarketing.util.json.JSONObject;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZonedDateTime;

/**
 * Validator that checks if a field value is a date in the format '2025-06-09T14:30:00+02:00'.
 * This validator will fail if the field value is not a string or not in the correct date format.
 */
public class DateValidator implements AnalyticsValidator {

    private static final String TYPE_ATTRIBUTE = "type";
    private static final String DATE_TYPE = "date";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Tests if this validator should be applied based on the validator configuration.
     * This validator is applied if the "type" attribute is set to "date".
     *
     * @param jsonValidatorBody The validator configuration
     * @return true if the validator should be applied, false otherwise
     */
    @Override
    public boolean test(JSONObject jsonValidatorBody) {
        return jsonValidatorBody.has(TYPE_ATTRIBUTE) && 
               DATE_TYPE.equals(jsonValidatorBody.get(TYPE_ATTRIBUTE));
    }

    /**
     * Validates that the field value is a date in the format '2025-06-09T14:30:00+02:00'.
     *
     * @param fieldValue The field value to validate
     * @throws AnalyticsValidationException if the field value is not a string or not in the correct date format
     */
    @Override
    public void validate(Object fieldValue) throws AnalyticsValidationException {
        if (fieldValue == null) {
            return; // Null values are handled by RequiredFieldValidator
        }
        
        if (!(fieldValue instanceof String)) {
            throw new AnalyticsValidationException(
                    String.format("Field value is not a String: %s", fieldValue),
                    ValidationErrorCode.INVALID_DATE_FORMAT
            );
        }
        
        String dateStr = (String) fieldValue;
        try {
            ZonedDateTime.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new AnalyticsValidationException(
                    String.format("Field value is not a valid date in format '2025-06-09T14:30:00+02:00': %s", dateStr),
                    ValidationErrorCode.INVALID_DATE_FORMAT
            );
        }
    }
}