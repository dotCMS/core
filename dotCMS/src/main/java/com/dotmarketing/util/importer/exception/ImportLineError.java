package com.dotmarketing.util.importer.exception;

import com.dotmarketing.util.importer.ImportLineValidationCodes;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * Interface that serves as a bridge between core dotCMS exceptions and the Import process.
 * 
 * <p>This interface allows core dotCMS exceptions (like {@code DotContentletValidationException}) 
 * to provide additional structured payload information that is specifically expected and consumed 
 * by the Import utilities and processes.</p>
 * 
 * <p>By implementing this interface, exceptions can provide:</p>
 * <ul>
 *   <li><strong>Error codes</strong> - Standardized validation codes from {@link ImportLineValidationCodes}</li>
 *   <li><strong>Field context</strong> - The specific field name that caused the validation error</li>
 *   <li><strong>Value context</strong> - The actual value that failed validation</li>
 *   <li><strong>Additional context</strong> - Extra metadata like field types, expected patterns, etc.</li>
 * </ul>
 * 
 * <p>This structured information enables the Import process to:</p>
 * <ul>
 *   <li>Generate detailed error reports with specific field and value information</li>
 *   <li>Map validation failures to specific import line items</li>
 *   <li>Provide meaningful feedback to users during bulk import operations</li>
 *   <li>Handle different types of validation errors consistently</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Core exception that implements ImportLineError
 * DotContentletValidationException exception = DotContentletValidationException
 *     .builder("Validation failed")
 *     .addRequiredField(titleField, "")
 *     .addPatternField(emailField, "invalid-email", "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
 *     .build();
 * 
 * // Import process can then access structured error information
 * String errorCode = exception.getCode();  // "REQUIRED_FIELD_MISSING"
 * String fieldName = exception.getField().orElse("unknown");  // "title"
 * String fieldValue = exception.getValue().orElse("");  // ""
 * Map<String, ?> context = exception.getContext().orElse(Map.of());  // {fieldType: "text", ...}
 * }</pre>
 * 
 * @see ImportLineValidationCodes
 * @see com.dotmarketing.portlets.contentlet.business.DotContentletValidationException
 */
public interface ImportLineError extends Serializable {

    /**
     * Returns the standardized error code for this validation failure.
     * 
     * <p>Error codes are typically from {@link ImportLineValidationCodes} and provide
     * a consistent way to categorize different types of validation errors during import operations.</p>
     * 
     * @return The error code, defaults to "UNKNOWN_ERROR" if not specified
     * @see ImportLineValidationCodes
     */
    default String getCode() { return ImportLineValidationCodes.UNKNOWN_ERROR.name(); }

    /**
     * Returns additional context information about the validation failure.
     * 
     * <p>Context may include details such as:</p>
     * <ul>
     *   <li>Field metadata (name, type, velocity variable name)</li>
     *   <li>Validation constraints (expected patterns, min/max values)</li>
     *   <li>Related entity information</li>
     *   <li>Any other relevant diagnostic information</li>
     * </ul>
     * 
     * @return Optional map containing context information, empty if no context is available
     */
    default Optional<Map<String, ? extends Object>> getContext(){ return Optional.empty(); }

    /**
     * Returns the name of the field that caused the validation failure.
     * 
     * <p>This is typically the velocity variable name of the field for content types,
     * enabling the import process to map errors back to specific columns or form fields.</p>
     * 
     * @return Optional field name, empty if the error is not field-specific
     */
    default Optional<String> getField() { return Optional.empty(); }

    /**
     * Returns the actual value that failed validation.
     * 
     * <p>This provides the specific input value that caused the validation error,
     * which is useful for generating detailed error messages and debugging import issues.</p>
     * 
     * @return Optional value that failed validation, empty if no specific value is associated
     */
    default Optional<String> getValue() { return Optional.empty(); }

}
