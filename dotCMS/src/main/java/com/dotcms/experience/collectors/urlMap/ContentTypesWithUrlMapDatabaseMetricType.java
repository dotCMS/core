package com.dotcms.experience.collectors.urlMap;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Collects the count of content types with a non-null detail page.
 */
public class ContentTypesWithUrlMapDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "CONTENT_TYPES_WITH_URL_MAP";
    }

    @Override
    public String getDescription() {
        return "Count of content types with URL maps";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.URL_MAPS;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(*) AS value " +
                "FROM structure WHERE url_map_pattern IS NOT null AND page_detail IS NOT null";
    }
}
