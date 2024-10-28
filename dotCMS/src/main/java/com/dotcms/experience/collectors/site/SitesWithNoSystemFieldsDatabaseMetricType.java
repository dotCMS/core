package com.dotcms.experience.collectors.site;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Collects the count of non-system fields in Host content type
 */
public class SitesWithNoSystemFieldsDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "NON_SYSTEM_FIELDS_ON_CONTENT_TYPE_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of non-system fields in Host content type";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.SITES;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT count(f.inode) AS value\n" +
                "FROM field f, structure s WHERE f.structure_inode = s.inode\n" +
                "AND s.name = 'Host'\n" +
                "AND f.fixed = false\n" +
                "AND field_type <> 'com.dotcms.contenttype.model.field.RowField'\n" +
                "AND field_type <> 'com.dotcms.contenttype.model.field.ColumnField'";
    }
}
