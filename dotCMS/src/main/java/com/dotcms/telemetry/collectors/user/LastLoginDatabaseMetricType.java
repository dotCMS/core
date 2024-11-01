package com.dotcms.telemetry.collectors.user;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;

/**
 * Collects the date of the last login.
 */
public class LastLoginDatabaseMetricType implements UsersDatabaseMetricType  {

    @Override
    public String getName() {
        return "LAST_LOGIN";
    }

    @Override
    public String getDescription() {
        return "Date of the last Login";
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
        return "SELECT to_char (lastlogindate,'HH12:MI:SS DD Mon YYYY') AS value " +
                "FROM user_ where " + USER_EXCLUDE + " ORDER BY lastlogindate DESC LIMIT 1";
    }
}
