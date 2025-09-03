package com.dotmarketing.util.importer;

/**
 * Enumeration of validation codes used to identify specific types of line validation issues during
 * the import process. These codes provide a standardized way to categorize and handle different
 * validation scenarios when processing individual lines from the import file.
 */
public enum ImportLineValidationCodes {

    UNKNOWN_ERROR,
    /**
     * Line does not contain all required columns
     */
    INCOMPLETE_LINE,

    /**
     * Invalid date format found
     */
    INVALID_DATE_FORMAT,

    /**
     * Required key field is missing
     */
    MISSING_KEY_FIELD,

    /**
     * Invalid category key found
     */
    INVALID_CATEGORY_KEY,

    /**
     * Invalid site/folder reference
     */
    INVALID_SITE_FOLDER_REF,

    /**
     * Invalid binary field URL
     */
    INVALID_BINARY_URL,

    /**
     * Invalid URL for file/image field in dotCMS
     */
    UNREACHABLE_URL_CONTENT,

    /**
     * Invalid file path for file/image field in dotCMS
     */
    INVALID_FILE_PATH,

    /**
     * Invalid image file type
     */
    INVALID_IMAGE_TYPE,

    /**
     * File not found in content repository
     */
    FILE_NOT_FOUND,

    /**
     * Content not found by identifier
     */
    CONTENT_NOT_FOUND,

    /**
     * Invalid parent folder for URL
     */
    INVALID_URL_FOLDER,

    /**
     * Invalid workflow action
     */
    INVALID_WORKFLOW_ACTION,

    /**
     * User lacks workflow permissions
     */
    WORKFLOW_PERMISSION_ERROR,

    /**
     * Duplicate unique field value
     */
    DUPLICATE_UNIQUE_VALUE,

    /**
     * Relationship validation error
     */
    RELATIONSHIP_VALIDATION_ERROR,

    /**
     * Relationship cardinality error
     */
    RELATIONSHIP_CARDINALITY_ERROR,

    /**
     * User lacks required permissions
     */
    PERMISSION_ERROR,

    /**
     * The language does not exist
     */
    LANGUAGE_NOT_FOUND,

    /**
     * Regular non-key field is missing
     */
    REQUIRED_FIELD_MISSING,

    /**
     * Invalid field value
     */
    VALIDATION_FAILED_PATTERN,

    /**
     * Invalid numeric field value
     */
    INVALID_NUMBER_FORMAT,

    /**
     * Invalid field type
     */
    INVALID_FIELD_TYPE,

    /**
     * Invalid Json
     */
    INVALID_JSON

}