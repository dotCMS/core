package com.dotmarketing.util.importer;

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
     * @return The header type found (Identifier, Workflow Action, etc)
     */
    SpecialHeaderType type();

    /**
     * @return Column index where this special header was found
     */
    int columnIndex();

    /**
     * Special header types supported in import
     */
    enum SpecialHeaderType {
        IDENTIFIER,
        WORKFLOW_ACTION,
        NONE
    }

}
