package com.dotcms.telemetry.collectors.user;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;

/**
 * Collect the count of Active User
 */
public class ActiveUsersDatabaseMetricType implements UsersDatabaseMetricType  {

    @Override
    public String getName() {
        return "ACTIVE_USERS_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of Active Users";
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
                "WHERE " + USER_EXCLUDE + " AND lastlogindate > now() - interval '1 month'";
    }
}
