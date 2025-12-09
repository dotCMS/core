package com.dotcms.telemetry.collectors.language;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the total count of languages
 */
@ApplicationScoped
@DashboardMetric(category = "system", priority = 1)
public class TotalLanguagesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of configured dotCMS Languages";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.LANGUAGES;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(*) AS value FROM language";
    }
}
