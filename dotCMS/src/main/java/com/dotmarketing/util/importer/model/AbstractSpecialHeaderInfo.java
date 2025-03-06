package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import org.immutables.value.Value;

/**
 * Contains information about special header types (Identifier, Workflow Action) and their column
 * positions in the CSV file.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = SpecialHeaderInfo.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractSpecialHeaderInfo extends Serializable {

    /**
     * @return The header name
     */
    String header();

    /**
     * @return Column index where this special header was found
     */
    int columnIndex();

}
