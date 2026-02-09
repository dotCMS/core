package com.dotcms.telemetry.collectors.user;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the date of the last login.
 */
@ApplicationScoped
@MetricsProfile({ProfileType.STANDARD, ProfileType.FULL})
@DashboardMetric(category = "user", priority = 3)
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
