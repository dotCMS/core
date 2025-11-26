package com.dotcms.model.site;


import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.QueryParam;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = GetSitesRequest.class)
public interface AbstractGetSitesRequest {

    @QueryParam("filter")
    @Nullable
    String filter();

    @QueryParam("showArchived")
    @Nullable
    Boolean showArchived();

    @QueryParam("showLive")
    @Nullable
    Boolean showLive();

    @Nullable
    @QueryParam("showSystem")
    Boolean showSystem();

    @Nullable
    @QueryParam("page")
    Integer page();

    @QueryParam("perPage")
    @Nullable
    Integer perPage();

}
