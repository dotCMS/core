package com.dotcms.rest.api.v1.drive;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = QueryFilters.class)
@JsonDeserialize(as = QueryFilters.class)
public interface AbstractQueryFilters {

    /**
     * By default, we filter folders. When text is provided but we can always override this.
     * @return boolean
     */
    @JsonProperty("filterFolders")
    @Value.Default
    default boolean filterFolders(){ return true; }

    /**
     * Text to search for.
     * @return String
     */
    @JsonProperty("text")
    String text();

}
