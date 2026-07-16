package com.dotcms.rest.api.v1.health;

import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for individual health check results.
 * Contains detailed information about specific health check execution.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityHealthCheckResultView extends ResponseEntityView<HealthCheckResult> {
    public ResponseEntityHealthCheckResultView(final HealthCheckResult entity) {
        super(entity);
    }
}