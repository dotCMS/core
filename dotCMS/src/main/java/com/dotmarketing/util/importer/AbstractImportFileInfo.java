package com.dotmarketing.util.importer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonDeserialize(as = ImportFileInfo.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractImportFileInfo extends Serializable {

    /**
     * @return The total number of rows found in the import file
     */
    int totalRows();

    /**
     * @return The number of rows successfully parsed from the import file
     */
    int parsedRows();

    /**
     * @return Detailed information about the validated headers in the import file
     */
    ImportHeaderInfo headerInfo();
}
