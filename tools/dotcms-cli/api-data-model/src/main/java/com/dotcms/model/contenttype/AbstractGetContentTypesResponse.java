package com.dotcms.model.contenttype;

import com.dotcms.model.AbstractAPIResponse;
import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = GetContentTypesResponse.class)
public interface AbstractGetContentTypesResponse extends AbstractAPIResponse<List<ContentType>> {

}
