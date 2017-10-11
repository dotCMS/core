package com.dotcms.rest.api.v1.page;

import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.structure.model.Structure;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class ContentletMixIn {

    @JsonIgnore abstract Permissionable getParentPermissionable();
    @JsonIgnore abstract public Structure getStructure() ;

}
