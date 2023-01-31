package com.dotcms.model.site;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = SiteView.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractSiteView extends AbstractSite {


    @JsonProperty("siteName")
    String hostName();


}
