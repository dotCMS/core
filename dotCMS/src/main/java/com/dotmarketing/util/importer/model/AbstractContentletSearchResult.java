package com.dotmarketing.util.importer.model;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import org.immutables.value.Value;

/**
 * Immutable data structure representing contentlet search results. This interface defines the
 * structure of the search results for contentlets, including the list of found contentlets, updated
 * inodes, and various metadata.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = ContentletSearchResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractContentletSearchResult extends Serializable {

    /**
     * Gets the list of contentlets that match the search criteria.
     *
     * @return a list of {@link Contentlet} objects.
     */
    List<Contentlet> contentlets();

    /**
     * Gets the list of inodes that were updated during the search.
     *
     * @return a list of updated inodes as strings.
     */
    List<String> updatedInodes();

    /**
     * Checks if the search results are multilingual.
     *
     * @return true if the results are multilingual, false otherwise.
     */
    boolean isMultilingual();

    /**
     * Gets the search condition values used for tracking duplicates.
     *
     * @return a string representing the search condition values.
     */
    String conditionValues();

    /**
     * Gets any validation messages generated during the search.
     *
     * @return a list of {@link ValidationMessage} objects.
     */
    List<ValidationMessage> messages();

}
