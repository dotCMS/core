package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import org.immutables.value.Value;

/**
 * Immutable data structure containing counts of valid and invalid records processed
 * during the import operation.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = ProcessedData.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractProcessedData extends Serializable {

    /**
     * @return The number of records that passed validation and were processed successfully
     */
    int valid();

    /**
     * @return The number of records that failed validation or could not be processed
     */
    int invalid();

}
