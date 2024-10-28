package com.dotcms.experience.collectors.language;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Collects the total count of languages
 */
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
