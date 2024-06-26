package com.dotcms.model.site;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = CopySiteRequest.class)
public interface AbstractCopySiteRequest {

    String  copyFromSiteId();
    Boolean copyAll();
    Boolean copyTemplatesContainers();
    Boolean copyContentOnPages();
    Boolean copyFolders();
    Boolean copyContentOnSite();
    Boolean copyLinks();
    Boolean copySiteVariables();
    CreateUpdateSiteRequest site();
    
}
