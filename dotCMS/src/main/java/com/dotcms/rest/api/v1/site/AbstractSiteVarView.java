package com.dotcms.rest.api.v1.site;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableSiteVarView.class)
@JsonDeserialize(as = ImmutableSiteVarView.class)
@Value.Immutable
public interface AbstractSiteVarView {

    String id();
    String name();
    String key();
    String value();

}
