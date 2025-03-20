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
     * @return The number of rows successfully parsed from the import file
     */
    int parsedRows();

    /**
     * Calculates and returns the number of rows in the import file
     * that failed to be successfully parsed or processed.
     *
     * @return The number of rows that failed to be processed
     */
    int failedRows();

}
