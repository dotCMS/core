package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for detailed role responses.
 * Contains comprehensive role information including child roles and hierarchy.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityRoleDetailView extends ResponseEntityView<RoleView> {
    public ResponseEntityRoleDetailView(final RoleView entity) {
        super(entity);
    }
}