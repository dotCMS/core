package com.dotcms.jitsu.validators;

/**
 * Enum representing validation error codes for analytics data validation. These codes are used to
 * categorize different types of validation errors that can occur during the validation process of
 * analytics events.
 *
 * @author Freddy Rodriguez
 * @since Jun 23rd, 2025
 */
public enum ValidationErrorCode {

    /**
     * Indicates that a required field is missing from the JSON payload.
     * This error occurs when a field marked as required in the validation configuration
     * is not present in the input data.
     */
    REQUIRED_FIELD_MISSING,

    /**
     * Indicates that a field expected to be a string is either empty or not a string.
     * This error occurs when validating fields that should contain string values.
     */
    INVALID_STRING_TYPE,

    /**
     * Indicates that a field expected to be a JSON object is not a valid JSON object.
     * This error occurs when validating fields that should contain nested JSON objects.
     */
    INVALID_JSON_OBJECT_TYPE,

    /**
     * Indicates that a field expected to be a JSON array is not a valid JSON array.
     * This error occurs when validating fields that should contain arrays or collections.
     */
    INVALID_JSON_ARRAY_TYPE,

    /**
     * Indicates that a field in the JSON payload is not defined in the validation configuration.
     * This error occurs when the input data contains fields that are not expected or allowed.
     */
    UNKNOWN_FIELD,

    /**
     * Indicates that a field expected to be a date is not in the correct format.
     * This error occurs when validating fields that should contain date values in the format '2025-06-09T14:30:00+02:00'.
     */
    INVALID_DATE_FORMAT,

    /**
     * Indicates that the provided Site Key is invalid, or does not match the expected format.
     */
    INVALID_SITE_AUTH,


    /**
     * Indicates that the maximum limit of custom attributes has been reached.
     */
    MAX_LIMIT_OF_CUSTOM_ATTRIBUTE_REACHED,
    /**
     * Indicates that a field expected to be a number is either empty or not a number.
     * This error occurs when validating fields that should contain number values.
     */
    INVALID_NUMBER_TYPE;

}
