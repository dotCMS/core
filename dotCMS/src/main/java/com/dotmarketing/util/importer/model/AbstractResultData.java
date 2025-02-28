package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import org.immutables.value.Value;

/**
 * Immutable data structure that combines processing statistics and content summary information.
 * This interface represents the complete data results of an import operation.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = ResultData.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractResultData extends Serializable {

    /**
     * @return Statistics about processed records including valid and invalid counts
     */
    ProcessedData processed();

    /**
     * @return Summary information about created and updated content
     */
    ContentSummary summary();

}
