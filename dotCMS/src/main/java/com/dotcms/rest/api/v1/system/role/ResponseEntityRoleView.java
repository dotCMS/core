package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;


/**
 * Wrapper for the response entity view output
 * @author jsanca
 */
public class ResponseEntityRoleView extends ResponseEntityView<RoleView> {

    public ResponseEntityRoleView(final RoleView entity) {
        super(entity);
    }
}
