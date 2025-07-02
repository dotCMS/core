package com.dotcms.rest.api.v1.system;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for configuration responses.
 * Contains system configuration properties and variables.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityConfigurationView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityConfigurationView(final Map<String, Object> entity) {
        super(entity);
    }
}