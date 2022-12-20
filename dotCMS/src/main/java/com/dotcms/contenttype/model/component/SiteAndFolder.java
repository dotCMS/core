package com.dotcms.contenttype.model.component;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableSiteAndFolder.class)
@JsonDeserialize(as = ImmutableSiteAndFolder.class)
@Value.Immutable
public interface SiteAndFolder {

    @Nullable
    String host();

    @Nullable
    String folder();

    @Nullable
    String siteName();

    @Nullable
    String folderPath();

}
