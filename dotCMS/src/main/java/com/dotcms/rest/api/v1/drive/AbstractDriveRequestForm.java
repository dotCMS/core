package com.dotcms.rest.api.v1.drive;

import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = DriveRequestForm.class)
@JsonDeserialize(as = DriveRequestForm.class)
public interface AbstractDriveRequestForm {

    String SORT_BY = "modDate";

    @JsonProperty("assetPath")
    String assetPath();

    @JsonProperty("includeSystemHost")
    @Value.Default
    default boolean includeSystemHost(){return true;}

    @JsonProperty("language")
    @Value.Default
    default List<String> language() { return List.of(APILocator.getLanguageAPI().getDefaultLanguage().toString());  }

    @Nullable
    @JsonProperty("contentTypes")
    List<String> contentTypes();

    @Nullable
    @JsonProperty("baseTypes")
    List<String> baseTypes();

    @Nullable
    @JsonProperty("mimeTypes")
    List<String> mimeTypes();

    @Nullable
    @JsonProperty("filters")
    QueryFilters filters();

    @JsonProperty("offset")
    @Value.Default
    default int offset(){ return 0; }

    @JsonProperty("maxResults")
    @Value.Default
    default int maxResults() { return 500; }

    @JsonProperty("sortBy")
    @Value.Default
    default String sortBy() { return SORT_BY; }

    @JsonProperty("live")
    @Value.Default
    default boolean live() { return false; }

    @JsonProperty("archived")
    @Value.Default
    default boolean archived() { return false; }
}
