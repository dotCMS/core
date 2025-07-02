package com.dotcms.rest.api.v1.health;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for health status responses.
 * Contains general health status information and configuration data.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityHealthStatusView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityHealthStatusView(final Map<String, Object> entity) {
        super(entity);
    }
}