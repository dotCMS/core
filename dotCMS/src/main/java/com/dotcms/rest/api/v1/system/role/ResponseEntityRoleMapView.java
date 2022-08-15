package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;
import java.util.Map;


/**
 * Wrapper for the response entity view output
 * @author jsanca
 */
public class ResponseEntityRoleMapView extends ResponseEntityView<List<Map<String, Object>>> {

    public ResponseEntityRoleMapView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}
