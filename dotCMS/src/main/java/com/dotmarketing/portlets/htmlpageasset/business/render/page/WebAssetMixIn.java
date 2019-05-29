package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.structure.model.Structure;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Restricts the JSON conversion of specific data in the {@link com.dotmarketing.portlets.templates.model.Template} object.
 *
 */
abstract class WebAssetMixIn {

    @JsonIgnore
    public abstract Permissionable getParentPermissionable();

    @JsonIgnore
    public abstract Structure getMap();

}
