package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;
import java.util.Map;

/**
 * Wrapper for the response entity view output
 * @author jsanca
 */
public class ResponseEntityPermissionGroupByTypeView extends ResponseEntityView<Map<String, List<String>>> {

    public ResponseEntityPermissionGroupByTypeView(final Map<String, List<String>> entity) {
        super(entity);
    }
}
