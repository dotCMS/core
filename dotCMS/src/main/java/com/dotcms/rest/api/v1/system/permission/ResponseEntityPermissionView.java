package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * Wrapper for the reponse entity view output
 * @author jsanca
 */
public class ResponseEntityPermissionView extends ResponseEntityView<List<PermissionView>> {

    public ResponseEntityPermissionView(List<PermissionView> entity) {
        super(entity);
    }
}
