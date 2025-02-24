package com.dotmarketing.util.importer.model;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.immutables.value.Value;

/**
 * Represents the result of processing a field from a single line during content import.
 * This immutable data structure contains field value, categories, site/folder information,
 * URL data, and validation results from processing a CSV line's field.
 *
 * <p>The interface uses Immutables.org to generate an immutable implementation with a builder pattern.
 * The generated class will be named {@code FieldProcessingResult}.</p>
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = FieldProcessingResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractFieldProcessingResult extends Serializable {

    /**
     * @return The value of the processed field
     */
    Optional<Object> value();

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
     * @return Unique field validation result from processing a field marked as unique
     */
    Optional<UniqueFieldBean> uniqueField();

    /**
     * @return The line number in the CSV file that produced these results
     */
    int lineNumber();

    /**
     * @return List of validation messages generated during field processing
     */
    List<ValidationMessage> messages();

}
