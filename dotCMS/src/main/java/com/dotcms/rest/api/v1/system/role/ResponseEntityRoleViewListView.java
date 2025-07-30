package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for role view list responses.
 * Contains collections of role views for hierarchical role display.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityRoleViewListView extends ResponseEntityView<List<RoleView>> {
    public ResponseEntityRoleViewListView(final List<RoleView> entity) {
        super(entity);
    }
}