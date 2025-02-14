package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import org.immutables.value.Value;

/**
 * Immutable data structure that holds summary information about content processing results.
 * This includes counts of created and updated content, as well as the content type identifier.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = ContentSummary.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractContentSummary extends Serializable {

    /**
     * @return The number of new content items created during the import process
     */
    int created();

    /**
     * @return The number of existing content items updated during the import process
     */
    int updated();

    /**
     * @return The identifier of the content type being processed
     */
    String contentType();

}
