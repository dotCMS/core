package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for role operation responses.
 * Contains operation results like role checks, deletions, and layout operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityRoleOperationView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityRoleOperationView(final Map<String, Object> entity) {
        super(entity);
    }
}