package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

/**
 * Immutable data structure representing the complete results of header validation.
 * Includes header information, validation messages, and contextual data needed for
 * the import process.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = HeaderValidationResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractHeaderValidationResult {

    /**
     * @return Detailed information about the validated headers
     */
    HeaderInfo headerInfo();

    /**
     * @return List of validation messages generated during header processing
     */
    List<ValidationMessage> messages();

    /**
     * @return Contextual information needed for the import process, such as processed headers,
     *         relationships, and field mappings
     */
    Map<String, Object> context();

}
