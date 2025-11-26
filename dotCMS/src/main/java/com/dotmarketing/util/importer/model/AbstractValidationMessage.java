package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Immutable data structure representing a validation message generated during the import process.
 * Messages can be errors, warnings, or informational, and may include contextual information
 * such as line numbers, field names, and invalid values.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = ValidationMessage.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public interface AbstractValidationMessage extends Serializable {

    /**
     * Enumeration of possible message types for validation results.
     */
    enum ValidationMessageType {
        /**
         * Indicates a critical error that prevents successful processing
         */
        ERROR,
        /**
         * Indicates a potential issue that doesn't prevent processing
         */
        WARNING,
        /**
         * Provides additional information about the process
         */
        INFO
    }

    /**
     * @return The type of validation message
     */
    @JsonIgnore
    ValidationMessageType type();

    /**
     * @return Optional validation code identifying the specific type of message
     */
    Optional<String> code();

    /**
     * @return The human-readable validation message
     */
    String message();

    /**
     * @return Optional line number in the import file where the issue was found
     */
    @JsonProperty("row")
    Optional<Integer> lineNumber();

    /**
     * @return Optional field name related to the validation message
     */
    Optional<String> field();

    /**
     * @return Additional contextual information about the validation
     */
    Map<String, Object> context();

    /**
     * @return Optional value that failed validation
     */
    @JsonProperty("value")
    Optional<String> invalidValue();
}
