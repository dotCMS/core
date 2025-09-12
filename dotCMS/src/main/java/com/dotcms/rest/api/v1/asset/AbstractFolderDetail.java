package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = FolderDetail.class)
@JsonDeserialize(as = FolderDetail.class)
public interface AbstractFolderDetail {

    @Nullable
    @JsonProperty("name")
    String name();

    @Nullable
    @JsonProperty("title")
    String title();

    @Nullable
    @JsonProperty("sortOrder")
    @Value.Default
    default Integer sortOrder() { return 0; }

    @Nullable
    @JsonProperty("showOnMenu")
    Boolean showOnMenu();

    @Nullable
    @JsonProperty("fileMasks")
    List<String> fileMasks();

    @Nullable
    @JsonProperty("defaultAssetType")
    String defaultAssetType();

}
