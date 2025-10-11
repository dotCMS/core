package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response entity wrapper for permission metadata endpoint.
 * Contains the available permission levels (READ, WRITE, PUBLISH, EDIT_PERMISSIONS,
 * CAN_ADD_CHILDREN) and permission scopes (HOST, FOLDER, CONTENT, TEMPLATE, etc.)
 * that can be assigned to users and roles in the dotCMS system.
 */
public class ResponseEntityPermissionMetadataView extends ResponseEntityView<PermissionMetadata> {

    /**
     * Constructs a new response wrapper for permission metadata.
     *
     * @param metadata The permission metadata containing levels and scopes
     */
    public ResponseEntityPermissionMetadataView(final PermissionMetadata metadata) {
        super(metadata);
    }
}