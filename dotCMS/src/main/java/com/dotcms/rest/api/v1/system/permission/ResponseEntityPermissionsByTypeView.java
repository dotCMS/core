package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * ResponseEntityView wrapper for permissions by type data.
 * Used by PermissionResource endpoints that return nested permission maps
 * organized by permission type and containing boolean permission values.
 */
public class ResponseEntityPermissionsByTypeView extends ResponseEntityView<Map<String, Map<String, Boolean>>> {
    
    public ResponseEntityPermissionsByTypeView(Map<String, Map<String, Boolean>> entity) {
        super(entity);
    }
}