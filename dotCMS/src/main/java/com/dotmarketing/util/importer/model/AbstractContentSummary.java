package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

/**
 * AbstractContentSummary represents a summary of content operation statistics,
 * including counts of created, updated, to be created, to be updated, and duplicate content.
 * It is an abstract representation meant to be extended by concrete implementations.
 * This class provides mechanisms to serialize and deserialize content summary data,
 * with JSON mappings for specific fields.
 *
 * Implementations of this interface are expected to capture various metrics related to
 * content management operations, allowing the encoding and decoding of the state of these metrics.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = ContentSummary.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractContentSummary extends Serializable {

    /**
     * Retrieves the count of content items that were successfully created during the import
     * process.
     *
     * @return The number of content items created
     */
    @JsonIgnore
    int createdContent();

    /**
     * Returns the count of content items that were updated during the operation. This value
     * represents the number of existing content entries successfully modified.
     *
     * @return The count of updated content items
     */
    @JsonIgnore
    int updatedContent();

    /**
     * Gets the count of content items to be created during the import operation. This represents
     * content items that do not currently exist and need to be created based on the import data.
     *
     * @return The number of content items marked for creation
     */
    @JsonIgnore
    int toCreateContent();

    /**
     * Gets the count of content items that are marked to be updated during the processing
     * operation. This represents content that requires updates due to changes detected in the
     * import data or other processing logic.
     *
     * @return The count of content items marked for update
     */
    @JsonIgnore
    int toUpdateContent();

    /**
     * Gets the count of duplicate content items encountered during processing. This represents
     * content that was identified as duplicate based on the import data.
     *
     * @return Count of duplicate content items
     */
    @JsonIgnore
    int duplicateContent();

    /**
     * Retrieves the count of content items that were successfully created during the import
     * operation. This value reflects the number of newly added content items.
     *
     * @return The count of content items created
     */
    @JsonProperty("created")
    int createdDisplay();

    /**
     * Retrieves the count of content items that were updated during the operation for display
     * purposes. This value might represent a user-facing view of the number of updated items.
     *
     * @return The count of content items updated for display purposes
     */
    @JsonProperty("updated")
    int updatedDisplay();

    /**
     * Retrieves the count of content items that failed during the operation. This represents the
     * number of items that encountered issues and could not be processed successfully.
     *
     * @return The count of failed content items
     */
    @JsonProperty("failed")
    int failedDisplay();

    /**
     * number of times a commit operation was performed
     * Mostly here for debugging/testing purposes
     * @return number of commits
     */
    @Default
    @JsonIgnore
    default int commits(){ return 0; }

    /**
     * number of times a rollback operation was performed
     * Mostly here for debugging/testing purposes
     * @return number of rollbacks
     */
    @Default
    @JsonIgnore
    default int rollbacks(){ return 0; }
}
