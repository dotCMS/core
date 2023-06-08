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
public interface AbstractSiteView  {

    @Nullable
    String inode();

    @Nullable
    String identifier();

    @Nullable
    String aliases();

    @Nullable
    @JsonProperty("systemHost")
    Boolean systemHost();

    @JsonProperty("siteName")
    String hostName();

    default String siteName() {
        return hostName();
    }

    @Nullable
    @JsonProperty("tagStorage")
    String tagStorage();

    @Nullable
    @JsonProperty("siteThumbnail")
    String siteThumbnail();

    @Nullable
    @JsonProperty("runDashboard")
    Boolean runDashboard();

    @Nullable
    @JsonProperty("keywords")
    String keywords();

    @Nullable
    @JsonProperty("description")
    String description();

    @Nullable
    @JsonProperty("googleMap")
    String googleMap();

    @Nullable
    @JsonProperty("googleAnalytics")
    String googleAnalytics();

    @Nullable
    @JsonProperty("addThis")
    String addThis();

    @Nullable
    @JsonProperty("proxyUrlForEditMode")
    String proxyUrlForEditMode();

    @Nullable
    @JsonProperty("embeddedDashboard")
    String embeddedDashboard();

    @JsonProperty("languageId")
    Long languageId();

    @Nullable
    @JsonProperty("default")
    Boolean isDefault();

    @Nullable
    @JsonProperty("archived")
    Boolean isArchived();

    @Nullable
    @JsonProperty("live")
    Boolean isLive();

    @Nullable
    @JsonProperty("working")
    Boolean isWorking();

    @Nullable
    @JsonProperty("locked")
    Boolean isLocked();

    @JsonProperty("modDate")
    Date modDate();

    @JsonProperty("modUser")
    String modUser();

}
