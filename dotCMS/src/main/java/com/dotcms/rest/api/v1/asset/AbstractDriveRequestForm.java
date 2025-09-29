package com.dotcms.rest.api.v1.asset;

import static com.dotcms.rest.api.v1.asset.WebAssetHelper.SORT_BY;

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

    @JsonProperty("assetPath")
    List<String> assetPath();

    @JsonProperty("language")
    @Value.Default
    default List<String> language() { return List.of(APILocator.getLanguageAPI().getDefaultLanguage().toString());  }

    @Nullable
    @JsonProperty("contentTypes")
    List<String> contentTypes();

    @Nullable
    @JsonProperty("baseTypes")
    List<String> baseTypes();

    @JsonProperty("showContent")
    @Value.Default
    default boolean showContent(){return true;}

    @JsonProperty("showImages")
    @Value.Default
    default boolean showImages(){return true;}

    @JsonProperty("showFiles")
    @Value.Default
    default boolean showFiles(){ return false; }

    @JsonProperty("showFolders")
    @Value.Default
    default boolean showFolders(){ return false; }

    @Nullable
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
    default int offset(){ return 0; }

    @JsonProperty("maxResults")
    @Value.Default
    default int maxResults() { return 500; }

    @JsonProperty("sortBy")
    default String sortBy() { return SORT_BY; }

    @JsonProperty("live")
    @Value.Default
    default boolean live() { return false; }

    @JsonProperty("working")
    @Value.Default
    default boolean showWorking(){ return false; }

}
