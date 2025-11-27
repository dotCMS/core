package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * Response wrapper for user permission endpoint.
 */
public class ResponseEntityUserPermissionsView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityUserPermissionsView(Map<String, Object> entity) {
        super(entity);
    }
}