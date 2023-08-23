package com.dotcms.model;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(builder = Pagination.Builder.class)
public interface AbstractPagination {

     @Value.Default
     default int currentPage() {return 0;}

    @Value.Default
    default int perPage(){return 0;}

    @Value.Default
    default long totalEntries(){ return 0;};


}
