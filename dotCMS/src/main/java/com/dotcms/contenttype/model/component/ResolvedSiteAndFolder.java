package com.dotcms.contenttype.model.component;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;


/**
 * I'm an Immutable  that basically tells APIs like ContentTypeAPI what site and folder the content-type should be live on
 */
@JsonSerialize(as = ImmutableResolvedSiteAndFolder.class)
@JsonDeserialize(as = ImmutableResolvedSiteAndFolder.class)
@Value.Immutable
public interface ResolvedSiteAndFolder {

    String resolvedSite();

    String resolvedFolder();

}
