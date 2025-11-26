package com.dotmarketing.util.importer.exception;

/**
 * Exception class specifically for handling validation errors during line-by-line content processing
 * in the import process. This exception is thrown when issues are detected with individual CSV lines
 * such as invalid field values, missing required data, or relationship validation failures.
 *
 * <p>This class extends ValidationMessageException to provide structured error information that can
 * be converted into ValidationMessage objects for consistent error handling and reporting. It includes
 * specific information about the line being processed when the error occurred.
 *
 * <p>Example usage:
 * <pre>
 * throw ImportLineException.builder()
 *     .message("Invalid category key found")
 *     .code("INVALID_CATEGORY")
 *     .lineNumber(42)
 *     .field("categoryField")
 *     .invalidValue("invalidKey")
 *     .build();
 * </pre>
 *
 * <p>This exception is typically thrown during the content processing phase of the import process,
 * when validating and importing individual lines from the CSV file.
 *
 * @see ValidationMessageException
 * @see com.dotmarketing.util.importer.model.ValidationMessage
 */
public class ImportLineException extends ValidationMessageException {

    private ImportLineException(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ValidationMessageException.Builder<Builder> {

        @Override
        public ImportLineException build() {
            validate();
            return new ImportLineException(this);
        }
    }
}
