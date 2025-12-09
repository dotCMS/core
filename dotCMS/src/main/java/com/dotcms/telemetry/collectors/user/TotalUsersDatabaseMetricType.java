package com.dotcms.telemetry.collectors.user;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the total count of all users (excluding system users)
 */
@ApplicationScoped
@DashboardMetric(category = "user", priority = 2)
public class TotalUsersDatabaseMetricType implements UsersDatabaseMetricType {
    @Override
    public String getName() {
        return "COUNT_OF_USERS";
    }

    @Override
    public String getDescription() {
        return "Total count of users";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.USERS;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(userid) as value FROM user_ " +
                "WHERE " + USER_EXCLUDE;
    }
}

