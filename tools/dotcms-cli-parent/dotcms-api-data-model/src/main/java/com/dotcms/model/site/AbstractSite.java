package com.dotcms.model.site;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = Site.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractSite {

    String inode();

    String identifier();

    @Nullable
    String aliases();

    @JsonProperty("hostname")
    String hostName();

    @Nullable
    @JsonProperty("systemHost")
    Boolean systemHost();

    @JsonProperty("default")
    Boolean isDefault();

    @JsonProperty("archived")
    Boolean isArchived();

    @JsonProperty("live")
    Boolean isLive();

    @JsonProperty("working")
    Boolean isWorking();


}
