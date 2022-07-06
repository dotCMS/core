package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;


/**
 * Wrapper for the response entity view output
 * @author jsanca
 */
public class ResponseEntityRoleView extends ResponseEntityView<List<RoleView>> {

    public ResponseEntityRoleView(final List<RoleView> entity) {
        super(entity);
    }
}
