package com.dotmarketing.util.importer.model;

import com.dotmarketing.portlets.categories.model.Category;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Represents the complete results of importing a single line from a CSV file.
 * This immutable data structure tracks all aspects of the import process including:
 * <ul>
 *   <li>Content creation/update status</li>
 *   <li>Validation results</li>
 *   <li>Content relationships</li>
 *   <li>Categories</li>
 *   <li>Processing counters</li>
 * </ul>
 *
 * <p>The interface uses Immutables.org to generate an immutable implementation with a builder pattern.
 * The generated class will be named {@code LineImportResult}.</p>
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = LineImportResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractLineImportResult extends Serializable {

    /**
     * Indicates whether this line created new content or updated existing content.
     *
     * @return true if new content was created, false if existing content was updated
     */
    @Value.Default
    default boolean isNewContent() {
        return false;
    }

    /**
     * Indicates whether this line should be skipped during import processing.
     * Lines may be ignored due to validation failures or data issues.
     *
     * @return true if the line should be ignored, false if it should be processed
     */
    @Value.Default
    default boolean ignoreLine() {
        return false;
    }

    /**
     * @return The line number in the CSV file being processed
     */
    int lineNumber();

    /**
     * @return Optional identifier of the content created or updated from this line
     */
    Optional<String> contentIdentifier();

    /**
     * @return Optional inode of the content created or updated from this line
     */
    Optional<String> contentInode();

    /**
     * @return List of inodes for all content updated during processing of this line
     */
    List<String> updatedInodes();

    /**
     * Gets the language ID for the imported content.
     * A value of -1 indicates the default language should be used.
     *
     * @return The language ID, or -1 for default language
     */
    @Value.Default
    default long languageId() {
        return -1L;
    }

    /**
     * @return Map of key field names to values used to match existing content
     */
    Map<String, String> keyFields();

    /**
     * @return List of validation and processing messages generated during import
     */
    List<ValidationMessage> messages();

    /**
     * @return List of categories assigned to the imported content
     */
    List<Category> categories();

    /**
     * @return List of inodes for all content saved during processing of this line
     */
    List<String> savedInodes();

    /**
     * @return The inode of the last content saved, if any
     */
    @Nullable
    String lastInode();

    /**
     * Gets the count of content items that are to be created during processing.
     * This represents content that is identified for creation based on the import data.
     *
     * @return Count of content items to be created
     */
    int contentToCreate();

    /**
     * Gets the count of content items that have been created during processing.
     * This represents content that has been successfully created.
     *
     * @return Count of content items created
     */
    int createdContent();

    /**
     * Gets the count of content items that are to be updated during processing.
     * This represents content that is identified for update based on the import data.
     *
     * @return Count of content items to be updated
     */
    int contentToUpdate();

    /**
     * Gets the count of content items that have been updated during processing.
     * This represents content that has been successfully updated.
     *
     * @return Count of content items updated
     */
    int updatedContent();

    /**
     * Gets the count of duplicate content items encountered during processing. This represents
     * content that was identified as duplicate based on the import data.
     *
     * @return Count of duplicate content items
     */
    int duplicateContent();

}
