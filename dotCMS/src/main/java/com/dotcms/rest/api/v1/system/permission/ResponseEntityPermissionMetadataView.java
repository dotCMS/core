package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

public class ResponseEntityPermissionMetadataView extends ResponseEntityView<Map<String, Object>> {
    
    public ResponseEntityPermissionMetadataView(Map<String, Object> metadata) {
        super(metadata);
    }
}