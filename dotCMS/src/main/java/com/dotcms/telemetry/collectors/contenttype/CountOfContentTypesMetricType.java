package com.dotcms.telemetry.collectors.contenttype;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the total number of content types (structures) in the system.
 *
 * <p>This metric counts ALL content types regardless of their workflow assignment,
 * providing a core indicator of content model complexity.</p>
 *
 * <p>Profile: MINIMAL - Fast query suitable for dashboard overview</p>
 */
@ApplicationScoped
@MetricsProfile({ProfileType.MINIMAL, ProfileType.STANDARD, ProfileType.FULL})
@DashboardMetric(category = "content", priority = 2)
public class CountOfContentTypesMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_OF_CONTENT_TYPES";
    }

    @Override
    public String getDescription() {
        return "Total number of content types (structures)";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.CONTENT_TYPES;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(*) AS value FROM structure";
    }
}
