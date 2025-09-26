package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * Response view for user permission endpoints.
 * Encapsulates user permissions data organized by assets (hosts/folders) with role information.
 * The response contains userId, roleId, and an assets array with detailed permission information
 * for each asset the user has permissions on.
 * 
 * @author Hassan
 */
public class ResponseEntityUserPermissionsView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityUserPermissionsView(Map<String, Object> entity) {
        super(entity);
    }
}