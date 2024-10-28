package com.dotcms.experience.collectors.content;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Collect the count of Contentlets that were edited less than a month ago
 */
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
