package com.dotcms.experience.collectors.user;

import com.dotcms.experience.MetricType;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Represents the MetaData of a User Metric that we want to collect from DataBase
 *
 * @see MetricType
 */
public interface UsersDatabaseMetricType extends DBMetricType {

    String USER_EXCLUDE="companyid<>'default' and userid<> 'system' and userid <> 'anonymous'";

}
