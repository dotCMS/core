package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = UpdateFolderDetail.class)
@JsonDeserialize(as = UpdateFolderDetail.class)
public interface AbstractUpdateFolderDetail extends AbstractFolderDetail {

    /**
     * This prop only made available for the update folder endpoint. Since the name is derived from the path
     * @return String
     */
    @Nullable
    @JsonProperty("name")
    String name();

}
