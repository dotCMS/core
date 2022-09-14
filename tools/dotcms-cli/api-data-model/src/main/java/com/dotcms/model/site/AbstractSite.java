package com.dotcms.model.site;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = Site.class)
public interface AbstractSite {

    String inode();

    String identifier();

    @Nullable
    String aliases();

    String hostname();

    boolean systemHost();

    @JsonProperty("default")
    boolean isDefault();
}
