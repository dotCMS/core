package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response entity wrapper for save user permissions endpoint.
 * Follows dotCMS pattern of specific ResponseEntity*View classes for REST endpoints.
 *
 * @author Hassan
 * @since 24.01
 */
public class ResponseEntitySaveUserPermissionsView extends ResponseEntityView<SaveUserPermissionsResponse> {

    /**
     * Constructs response wrapper for save operation.
     *
     * @param entity The save response containing updated permissions
     */
    public ResponseEntitySaveUserPermissionsView(final SaveUserPermissionsResponse entity) {
        super(entity);
    }
}
