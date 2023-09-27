package com.dotcms.model.site;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = GetSiteByNameRequest.class)
public interface AbstractGetSiteByNameRequest {
    String siteName();

}
