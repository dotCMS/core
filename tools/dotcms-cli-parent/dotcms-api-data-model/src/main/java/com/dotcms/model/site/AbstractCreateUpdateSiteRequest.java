package com.dotcms.model.site;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ValueType
@Value.Immutable
@JsonDeserialize(as = CreateUpdateSiteRequest.class)
public interface AbstractCreateUpdateSiteRequest {

     String siteName();

     @Nullable
     String identifier();

     @Nullable
     String inode();

     @Nullable
     String aliases();

     @Nullable
     String tagStorage();

     @Nullable
     String siteThumbnail();

     @Nullable
     Boolean runDashboard();

     @Nullable
     String keywords();

     @Nullable
     String description();

     @Nullable
     String googleMap();

     @Nullable
     String googleAnalytics();

     @Nullable
     String addThis();

     @Nullable
     String proxyUrlForEditMode();

     @Nullable
     String embeddedDashboard();

     @Nullable
     Long languageId();

     @Nullable
     Boolean forceExecution();

}
