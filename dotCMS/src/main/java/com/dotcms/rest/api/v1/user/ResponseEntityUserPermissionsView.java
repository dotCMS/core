package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

public class ResponseEntityUserPermissionsView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityUserPermissionsView(Map<String, Object> entity) {
        super(entity);
    }
}