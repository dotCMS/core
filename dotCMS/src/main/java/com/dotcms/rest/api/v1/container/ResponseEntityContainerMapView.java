package com.dotcms.rest.api.v1.container;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for container map responses.
 * Contains map data for container operations like form rendering and content operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityContainerMapView extends ResponseEntityView<Map<String, String>> {
    public ResponseEntityContainerMapView(final Map<String, String> entity) {
        super(entity);
    }
}