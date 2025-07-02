package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for navigation hierarchy responses.
 * Contains hierarchical navigation structure and metadata for menu items.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityNavigationView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityNavigationView(final Map<String, Object> entity) {
        super(entity);
    }
}