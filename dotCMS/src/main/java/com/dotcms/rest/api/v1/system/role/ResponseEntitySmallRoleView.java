package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;


/**
 * Wrapper for the response entity view output
 * @author jsanca
 */
public class ResponseEntitySmallRoleView extends ResponseEntityView<List<SmallRoleView>> {

    public ResponseEntitySmallRoleView(final List<SmallRoleView> entity) {
        super(entity);
    }
}
