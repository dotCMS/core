package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = UpdateFolderDetail.class)
@JsonDeserialize(as = UpdateFolderDetail.class)
@Schema(description = "Folder update details including all base folder properties plus optional name change")
public interface AbstractUpdateFolderDetail extends AbstractFolderDetail {

    /**
     * This prop only made available for the update folder endpoint. Since the name is derived from the path
     * @return String
     */
    @Nullable
    @JsonProperty("name")
    @Schema(
        description = "New name for the folder. If provided, the folder will be renamed. The name is derived from the path if not specified",
        example = "my-renamed-folder",
        required = false
    )
    String name();

}
