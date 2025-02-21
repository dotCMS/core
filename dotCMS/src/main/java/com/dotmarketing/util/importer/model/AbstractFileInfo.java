package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import org.immutables.value.Value;

/**
 * Immutable data structure containing file processing information during import.
 * Tracks the total number of rows in the file and how many were successfully parsed,
 * along with header validation information.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = FileInfo.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractFileInfo extends Serializable {

    /**
     * @return The total number of rows found in the import file
     */
    int totalRows();

    /**
     * @return Detailed information about the validated headers in the import file
     */
    @JsonProperty("headers")
    HeaderInfo headerInfo();
}
