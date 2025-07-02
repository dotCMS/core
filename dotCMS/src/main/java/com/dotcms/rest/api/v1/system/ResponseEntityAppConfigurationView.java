package com.dotcms.rest.api.v1.system;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for application configuration responses.
 * Contains system properties and configuration parameters for the dotCMS Angular UI.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityAppConfigurationView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityAppConfigurationView(final Map<String, Object> entity) {
        super(entity);
    }
}