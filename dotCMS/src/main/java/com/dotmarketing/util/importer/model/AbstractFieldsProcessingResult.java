package com.dotmarketing.util.importer.model;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.immutables.value.Value;

/**
 * Represents the result of processing fields from a single line during content import.
 * This immutable data structure contains field values, categories, site/folder information,
 * URL data, and validation results from processing a CSV line's fields.
 *
 * <p>The interface uses Immutables.org to generate an immutable implementation with a builder pattern.
 * The generated class will be named {@code FieldProcessingResult}.</p>
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = FieldsProcessingResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractFieldsProcessingResult extends Serializable {

    /**
     * @return Map of column indices to their processed field values
     */
    Map<Integer, Object> values();

    /**
     * @return Set of categories extracted from category fields
     */
    Set<Category> categories();

    /**
     * @return Optional pair containing the Host and Folder for the content,
     *         if site/folder fields were processed
     */
    Optional<Pair<Host, Folder>> siteAndFolder();

    /**
     * @return Optional pair containing the column index and URL value for URL fields,
     *         present if a URL field was processed
     */
    Optional<Pair<Integer, String>> urlValue();

    /**
     * @return Optional asset name extracted from URL processing,
     *         present if a URL field was processed
     */
    Optional<String> urlValueAssetName();

    /**
     * @return List of unique field validation results from processing fields
     *         marked as unique in the content type
     */
    List<UniqueFieldBean> uniqueFields();

    /**
     * Indicates whether this line should be ignored during import.
     * Lines may be ignored due to validation failures or other processing issues.
     *
     * @return true if the line should be ignored, false otherwise
     */
    @Value.Default
    default boolean ignoreLine() {
        return false;
    }

    /**
     * @return The line number in the CSV file that produced these results
     */
    int lineNumber();

    /**
     * @return List of validation messages generated during field processing
     */
    List<ValidationMessage> messages();

}
