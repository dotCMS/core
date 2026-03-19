package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

/**
 * Typed ResponseEntityView for PermissionableObjectView.
 * Used for accurate Swagger schema documentation.
 */
public class ResponseEntityPermissionableObjectView extends ResponseEntityView<PermissionableObjectView> {

    public ResponseEntityPermissionableObjectView(final PermissionableObjectView entity) {
        super(entity);
    }
}
