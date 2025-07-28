package com.dotcms.rest.api.v1.site;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for site switch operation responses.
 * Contains information about whether the site switch was successful.
 * 
 * @author Steve Bolton
 */
public class ResponseEntitySiteSwitchView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntitySiteSwitchView(final Map<String, Object> entity) {
        super(entity);
    }
}