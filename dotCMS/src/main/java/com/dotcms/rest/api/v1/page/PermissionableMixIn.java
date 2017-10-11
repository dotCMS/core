package com.dotcms.rest.api.v1.page;

import com.dotmarketing.business.Permissionable;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class PermissionableMixIn {
    @JsonIgnore abstract Permissionable getParentPermissionable(); // we don't need it!
}