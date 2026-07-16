package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.business.Role;
import java.util.List;

/**
 * Entity View for role list responses.
 * Contains collections of roles for listing and filtering operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityRoleListView extends ResponseEntityView<List<Role>> {
    public ResponseEntityRoleListView(final List<Role> entity) {
        super(entity);
    }
}