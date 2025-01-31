package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import org.immutables.value.Value;

/**
 * Immutable data structure representing the complete results of an import operation.
 * Contains file information, data processing results, and validation messages.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = ImportResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractImportResult extends Serializable {

    /**
     * @return Information about the processed import file
     */
    FileInfo fileInfo();

    /**
     * @return Results of the data processing operation
     */
    ResultData data();

    /**
     * @return List of validation and processing messages generated during import
     */
    List<ValidationMessage> messages();

}
