package com.dotcms.model.site;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = SiteVariableView.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractSiteVariableView {

    String name();

    String key();

    String value();

}
