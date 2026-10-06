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

    /**
     * Which fields {@link #text()} is matched against.
     *
     * <p>Sits here rather than at the top level of the request because it qualifies {@code text}
     * and means nothing without it — the same reason {@link #filterFolders()} lives here. A request
     * carrying a search scope with no text is rejected as the contract error it is, rather than
     * being silently ignored.</p>
     *
     * <p>Defaults to {@link SearchScope#ALL_FIELDS}, so a request that omits this field is
     * processed exactly as it was before the field existed.</p>
     *
     * @return the {@link SearchScope}, never {@code null}
     */
    @JsonProperty("searchScope")
    @Value.Default
    default SearchScope searchScope() { return SearchScope.ALL_FIELDS; }

}
