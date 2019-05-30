package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotmarketing.business.Permissionable;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Restricts the JSON conversion of specific data in the Permissionable object.
 *
 * @author Will Ezell
 * @version 4.2
 * @since Oct 9, 2017
 */
abstract class PermissionableMixIn {

    @JsonIgnore
    abstract Permissionable getParentPermissionable();

}