package com.dotcms.rest.api.v1.drive;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = QueryFilters.class)
@JsonDeserialize(as = QueryFilters.class)
public interface AbstractQueryFilters {

    String text();

}
