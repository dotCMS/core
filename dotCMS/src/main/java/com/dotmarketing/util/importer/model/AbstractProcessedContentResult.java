package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Represents the results of content processing during import operations.
 * This immutable data structure tracks the outcomes of content creation,
 * updates, and validation during the import process.
 *
 * <p>Tracks inodes of saved content, maintains counters for different types
 * of operations (new, updated, duplicate), and collects validation messages
 * generated during processing.</p>
 *
 * <p>The interface uses Immutables.org to generate an immutable implementation
 * with a builder pattern. The generated class will be named {@code ProcessedContentResult}.</p>
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = ProcessedContentResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractProcessedContentResult extends Serializable {

    /**
     * Gets the list of inodes for all content successfully saved during processing.
     * This includes both newly created and updated content.
     *
     * @return List of inodes for saved content
     */
    List<String> savedInodes();

    /**
     * Gets the inode of the most recently saved content item.
     * This may be null if no content was saved during processing.
     *
     * @return The last saved inode, or null if no content was saved
     */
    @Nullable
    String lastInode();

    /**
     * Gets the count of content items that are to be created during processing. This represents
     * content that is identified for creation based on the import data.
     *
     * @return Count of content items to be created
     */
    int contentToCreate();

    /**
     * Gets the count of content items that were created during processing. This represents content
     * that was created, regardless of whether it was new or updated.
     *
     * @return Count of created content items
     */
    int createdContent();

    /**
     * Gets the count of content items that are to be updated during processing. This represents
     * content that is identified for update based on the import data.
     *
     * @return Count of content items to be updated
     */
    int contentToUpdate();

    /**
     * Gets the count of existing content items that were updated during processing.
     * This represents content that was matched by key fields and modified.
     *
     * @return Count of updated content items
     */
    int updatedContent();

    /**
     * Gets the count of duplicate content items encountered during processing.
     * This represents content that was identified as a duplicate based on
     * unique field constraints or other validation rules.
     *
     * @return Count of duplicate content items
     */
    int duplicateContent();

    /**
     * Gets the list of validation messages generated during content processing.
     * This includes warnings, errors, and informational messages about the
     * processing operations.
     *
     * @return List of validation messages from processing
     */
    List<ValidationMessage> messages();
}
