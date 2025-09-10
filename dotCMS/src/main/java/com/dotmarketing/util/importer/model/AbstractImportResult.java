package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Immutable data structure representing the complete results of an import operation.
 * Contains file information, data processing results, and validation messages.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = ImportResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractImportResult extends Serializable {

    /**
     * Retrieves the operation type.
     *
     * @return the operation type as an instance of OperationType
     */
    OperationType type();

    /**
     * Retrieves the name of the content type associated with the current import operation.
     *
     * @return the content type name as a string
     */
    @JsonInclude(Include.NON_EMPTY)
    String contentTypeName();

    /**
     * Retrieves the variable name associated with the content type in the import process.
     *
     * @return a String representing the variable name for the content type
     */
    @JsonInclude(Include.NON_EMPTY)
    String contentTypeVariableName();

    /**
     * Retrieves the identifier of the workflow action associated with the import operation.
     *
     * @return a string representing the workflow action ID
     */
    @JsonInclude(Include.NON_EMPTY)
    Optional<String> workflowActionId();

    /**
     * Retrieves a list of key fields used during the import operation. These fields typically
     * represent unique identifiers or critical attributes necessary for processing the data.
     *
     * @return a list of key field names as strings
     */
    List<String> keyFields();

    /**
     * @return Information about the processed import file
     */
    @JsonInclude(Include.NON_EMPTY)
    @JsonProperty("file")
    Optional<FileInfo> fileInfo();

    /**
     * @return Results of the data processing operation
     */
    @JsonInclude(Include.NON_EMPTY)
    Optional<ResultData> data();

    /**
     * Retrieves a list of validation messages, typically representing potential issues or
     * informational messages encountered during a validation process.
     *
     * @return a list of validation messages
     */
    List<ValidationMessage> info();

    /**
     * Retrieves a list of validation messages classified as warnings during the import operation.
     *
     * @return a list of warning messages encountered, or an empty list if no warnings are present
     */
    List<ValidationMessage> warning();

    /**
     * Retrieves a list of validation error messages.
     *
     * @return a list of ValidationMessage objects representing the errors encountered during
     * validation.
     */
    List<ValidationMessage> error();

    /**
     * Retrieves the identifier of the last inode processed during the import operation, if
     * available.
     *
     * @return an {@code Optional<String>} representing the last inode processed, or an empty
     * Optional if no inode was processed
     */
    @JsonIgnore
    Optional<String> lastInode();

    /**
     * Retrieves the flag indicating if the process stopped because an error was found and the configuration
     * @return an {@code Optional<Integer>} representing the last line were the error occurred or an empty
     * Optional empty if no error occurred or if
     */
    @JsonInclude(Include.NON_EMPTY)
    Optional<Integer> stoppedOnErrorAtLine();

    /**
     * Represents the type of operation being performed during an import process. This enum is used
     * to differentiate between different kinds of operations that can be performed, such as
     * publishing or previewing content.
     */
    enum OperationType {
        PUBLISH,
        PREVIEW
    }
}
