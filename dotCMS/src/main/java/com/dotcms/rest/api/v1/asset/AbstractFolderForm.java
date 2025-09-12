package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = FolderForm.class)
@JsonDeserialize(as = FolderForm.class)
public interface AbstractFolderForm {

    @JsonProperty("assetPath")
    String assetPath();

    @JsonProperty("data")
    AbstractFolderDetail data();
}
