package com.dotcms.rest.api.v1.experiments;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for experiment health check responses.
 * Contains health status information for experiment service connectivity.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityExperimentHealthView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityExperimentHealthView(final Map<String, Object> entity) {
        super(entity);
    }
}