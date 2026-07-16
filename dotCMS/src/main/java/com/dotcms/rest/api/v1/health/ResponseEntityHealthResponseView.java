package com.dotcms.rest.api.v1.health;

import com.dotcms.health.model.HealthResponse;
import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for health response status.
 * Contains overall system health information including check results and status.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityHealthResponseView extends ResponseEntityView<HealthResponse> {
    public ResponseEntityHealthResponseView(final HealthResponse entity) {
        super(entity);
    }
}