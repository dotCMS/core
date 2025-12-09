package com.dotcms.telemetry.collectors.content;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

import javax.enterprise.context.ApplicationScoped;

/**
 * Collect the count of Contentlets that were edited less than a month ago
 */
@ApplicationScoped
@DashboardMetric(category = "content", priority = 2)
public class RecentlyEditedContentDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "CONTENTS_RECENTLY_EDITED";
    }

    @Override
    public String getDescription() {
        return "Count of Contentlets that were Edited less than a month ago";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.CONTENTLETS;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(working_inode) AS value FROM contentlet_version_info, contentlet " +
                "WHERE contentlet.inode = contentlet_version_info.working_inode AND " +
                "contentlet.mod_date  > now() - interval '1 month'";
    }
}
