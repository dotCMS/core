package com.dotmarketing.util.importer;

/**
 * Enumeration of validation codes used to identify specific types of header validation issues
 * during the import process. These codes provide a standardized way to categorize and handle
 * different validation scenarios.
 */
public enum HeaderValidationCodes {

    /** Indicates a header that doesn't match any content type field */
    INVALID_HEADER,
    /**
     * Indicates no header was found in the CSV file
     */
    HEADERS_NOT_FOUND,
    /**
     * Indicates there is a missing header in the CSV file
     */
    MISSING_HEADER,
    /** Indicates a header name that appears more than once */
    DUPLICATE_HEADER,
    /** Indicates not all required content type fields are present in headers */
    INCOMPLETE_HEADERS,
    /** Indicates a required field is missing from the headers */
    REQUIRED_FIELD_MISSING,
    /** Indicates no key fields were specified for content matching */
    NO_KEY_FIELDS,
    /** Indicates a field marked as unique in the content type */
    UNIQUE_FIELD,
    /** Indicates an invalid key field specification */
    INVALID_KEY_FIELD

}
