package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

/**
 * Immutable data structure containing header validation information from the import file.
 * Tracks valid, invalid, and missing headers, along with any additional validation details.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = HeaderInfo.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractHeaderInfo extends Serializable {

    /**
     * @return The total number of headers found in the import file
     */
    int totalHeaders();

    /**
     * @return Array of headers that were successfully validated against the content type
     */
    String[] validHeaders();

    /**
     * @return Array of headers that did not match any content type fields
     */
    String[] invalidHeaders();

    /**
     * @return Array of required content type fields not found in the headers
     */
    String[] missingHeaders();

    /**
     * @return Additional validation details and metadata about the headers
     */
    Map<String, String> validationDetails();

    /**
     * @return Special header types found in the import file
     */
    List<SpecialHeaderInfo> specialHeaders();

}
