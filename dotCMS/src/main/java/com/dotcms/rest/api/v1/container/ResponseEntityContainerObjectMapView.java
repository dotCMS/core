package com.dotcms.rest.api.v1.container;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for container object map responses.
 * Contains generic map data for container operations with mixed value types.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityContainerObjectMapView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityContainerObjectMapView(final Map<String, Object> entity) {
        super(entity);
    }
}