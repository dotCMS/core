package com.dotcms.rest.api.v1.site;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@JsonSerialize(as = SimpleSiteVarView.class)
@JsonDeserialize(as = SimpleSiteVarView.class)
@Value.Immutable
public interface AbstractSimpleSiteVarView {

    String id();
    String name();
    String key();
    String value();

}
