package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response entity wrapper for save user permissions endpoint.
 * Follows dotCMS pattern of specific ResponseEntity*View classes for REST endpoints.
 *
 * @author hassandotcms
 */
public class ResponseEntitySaveUserPermissionsView extends ResponseEntityView<SaveUserPermissionsView> {

    /**
     * Constructs response wrapper for save operation.
     *
     * @param entity The save view containing updated permissions
     */
    public ResponseEntitySaveUserPermissionsView(final SaveUserPermissionsView entity) {
        super(entity);
    }
}
