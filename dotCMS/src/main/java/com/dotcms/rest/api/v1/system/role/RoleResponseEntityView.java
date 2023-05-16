package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * View for returning a role
 * @author jsanca
 */
public class RoleResponseEntityView extends ResponseEntityView<Map<String, Object>> {

    public RoleResponseEntityView(final  Map<String, Object>  entity) {
        super(entity);
    }
}
