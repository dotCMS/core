package com.dotcms.model.contenttype;


import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = FilterContentTypesRequest.class)
public interface AbstractFilterContentTypesRequest {

    @Nullable
    Map<String, Object> filter();

    @Nullable
    Integer page();

    @Nullable
    Integer perPage();

    @Nullable
    String orderBy();

    @Nullable
    String direction();

}
