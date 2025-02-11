package com.dotmarketing.util.importer.exception;

/**
 * Exception class specifically for handling validation errors during header processing in the content
 * import process. This exception is thrown when issues are detected with CSV headers such as missing
 * required headers, invalid header formats, or duplicate headers.
 *
 * <p>This class extends ValidationMessageException to provide structured error information that can
 * be converted into ValidationMessage objects for consistent error handling and reporting.
 *
 * <p>Example usage:
 * <pre>
 * throw HeaderValidationException.builder()
 *     .message("Required header 'title' not found")
 *     .code("MISSING_REQUIRED_HEADER")
 *     .field("title")
 *     .build();
 * </pre>
 *
 * <p>This exception is typically thrown during the header validation phase of the import process,
 * before any content processing begins.
 *
 * @see ValidationMessageException
 * @see com.dotmarketing.util.importer.HeaderValidationCodes
 */
public class HeaderValidationException extends ValidationMessageException {

    private HeaderValidationException(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ValidationMessageException.Builder<Builder> {
        @Override
        public HeaderValidationException build() {
            validate();
            return new HeaderValidationException(this);
        }
    }
}
