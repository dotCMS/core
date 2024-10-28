package com.dotcms.experience.collectors.user;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;

/**
 * Email address of the last logged-in user
 */
public class LastLoginUserDatabaseMetric implements UsersDatabaseMetricType  {

    @Override
    public String getName() {
        return "LAST_LOGIN_USER";
    }

    @Override
    public String getDescription() {
        return "Email address of the Last login User";
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
        return  "SELECT emailaddress AS value " +
                "FROM user_ where " + USER_EXCLUDE + " ORDER BY lastlogindate DESC LIMIT 1";
    }
}
