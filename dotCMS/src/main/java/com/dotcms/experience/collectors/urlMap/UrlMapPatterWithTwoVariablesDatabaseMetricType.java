package com.dotcms.experience.collectors.urlMap;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Collect the count of Content Types that are using a detail page with 2 or more variables.
 */
public class UrlMapPatterWithTwoVariablesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "COUNT_URL_MAP_PATTER_WITH_MORE_THAT_ONE_VARIABLE";
    }

    @Override
    public String getDescription() {
        return "Count of URL Map Patterns with more than one variable";
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
        return "SELECT count(*) AS value FROM structure " +
                "WHERE  page_detail IS NOT null AND REGEXP_COUNT(url_map_pattern, '\\{[^}]*\\}') >= 2";
    }
}

