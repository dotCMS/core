package com.dotcms.model.site;

import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.views.CommonViews;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import java.util.List;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = SiteView.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractSiteView  {

    String TYPE = "Site";

    @JsonView(CommonViews.InternalView.class)
    @Value.Derived
    default String dotCMSObjectType() {
        return TYPE;
    }

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

    @Nullable
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
    @JsonProperty("variables")
    List<SiteVariableView> variables();

    @Nullable
    @JsonProperty("locked")
    Boolean isLocked();

    @Nullable
    @JsonProperty("modDate")
    Date modDate();

    @Nullable
    @JsonProperty("modUser")
    String modUser();

}
