package com.dotcms.rest.api.v1.page;

import com.dotmarketing.business.Permissionable;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Restricts the JSON conversion of specific data in the Permissionable object.
 *
 * @author Will Ezell
 * @version 4.2
 * @since Oct 9, 2017
 */
public abstract class PermissionableMixIn {

    @JsonIgnore
    abstract Permissionable getParentPermissionable();

}