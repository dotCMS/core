package com.dotcms.telemetry.collectors.language;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

/**
 * Collects the count of unique configured dotCMS Languages, it means that if two o more Countries use the same
 * language then it just count as one
 */
@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
public class TotalUniqueLanguagesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "UNIQUE_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of unique configured dotCMS Languages, it means that if two o more Countries use the same " +
                "language then it just count as one";
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
        return "SELECT COUNT(DISTINCT language_code) AS value FROM language";
    }
}
