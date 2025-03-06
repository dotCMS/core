package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
     * @return Array of headers found in the import file
     */
    @JsonProperty("valid")
    String[] validHeaders();

    /**
     * @return Array of headers that did not match any content type fields
     */
    @JsonProperty("invalid")
    String[] invalidHeaders();

    /**
     * @return Array of required content type fields not found in the headers
     */
    @JsonProperty("missing")
    String[] missingHeaders();

    /**
     * @return Special header types found in the import file
     */
    @JsonProperty("special")
    List<SpecialHeaderInfo> specialHeaders();

    /**
     * @return Contextual information needed for the import process, such as processed headers,
     * relationships, and field mappings
     */
    @JsonIgnore
    Map<String, Object> context();

}
