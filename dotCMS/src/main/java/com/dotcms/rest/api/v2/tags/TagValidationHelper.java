package com.dotcms.rest.api.v2.tags;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.exception.BadRequestException;
import com.liferay.util.StringPool;
import java.util.List;

/**
 * Shared validation helper for v2 tag forms.
 * Provides consistent validation rules and structured error responses for all v2 tag operations.
 */
public class TagValidationHelper {
    
    private static final int MAX_TAG_LENGTH = 255;
    
    /**
     * Validates tag name according to dotCMS business rules.
     * 
     * @param tagName   The tag name to validate
     * @param fieldName The field name for error reporting
     * @throws BadRequestException if validation fails with structured error details
     */
    public static void validateTagName(String tagName, String fieldName) {
        if (tagName.contains(StringPool.COMMA)) {
            throwStructuredError("Tag name cannot contain commas", fieldName);
        }
        
        if (tagName.trim().isEmpty() || tagName.length() > MAX_TAG_LENGTH) {
            throwStructuredError("Tag name must be between 1 and 255 characters", fieldName);
        }
    }
    
    /**
     * Creates structured error response for consistent API error handling.
     */
    private static void throwStructuredError(String message, String fieldName) {
        final List<ErrorEntity> errors = List.of(
            new ErrorEntity("tag.validation.error", message, fieldName)
        );
        throw new BadRequestException(null, errors, message);
    }
}