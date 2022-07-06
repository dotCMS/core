package com.dotcms.model.site;

import com.dotcms.model.AbstractAPIResponse;
import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = GetSitesResponse.class)
public interface AbstractGetSitesResponse extends AbstractAPIResponse <List<Site>> {

}
