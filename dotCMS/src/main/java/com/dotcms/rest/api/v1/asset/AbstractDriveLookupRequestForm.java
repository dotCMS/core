package com.dotcms.rest.api.v1.asset;

import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = DriveLookupRequestForm.class)
@JsonDeserialize(as = DriveLookupRequestForm.class)
public interface AbstractDriveLookupRequestForm {

    @JsonProperty("assetPath")
    List<String> assetPath();

    @JsonProperty("language")
    @Value.Default
    default List<String> language() { return List.of(APILocator.getLanguageAPI().getDefaultLanguage().toString());  };

    @JsonProperty("contentTypes")
    List<String> contentTypes();

    @JsonProperty("baseTypes")
    List<String> baseTypes();

    @JsonProperty("showFiles")
    @Value.Default
    default boolean showFiles(){ return false; }

    @JsonProperty("showFolders")
    @Value.Default
    default boolean showFolders(){ return false; }

    @JsonProperty("filter")
    String filter();

    @JsonProperty("showArchived")
    @Value.Default
    default boolean showArchived(){ return false; }

    @JsonProperty("showPages")
    @Value.Default
    default boolean showPages(){ return false; }

    @JsonProperty("showDotAssets")
    @Value.Default
    default boolean showDotAssets(){ return false; }

    @JsonProperty("showLinks")
    @Value.Default
    default boolean showLinks(){ return false; }

    @JsonProperty("offset")
    @Value.Default
    default int offset(){ return 0; };

    @JsonProperty("maxResults")
    @Value.Default
    default int maxResults() { return 500; }

    @JsonProperty("sortBy")
    String sortBy();

    @JsonProperty("live")
    @Value.Default
    default boolean live() { return false; }

    @JsonProperty("working")
    @Value.Default
    default boolean showWorking(){ return false; }

}
