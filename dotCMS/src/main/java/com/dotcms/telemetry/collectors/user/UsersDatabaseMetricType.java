package com.dotcms.telemetry.collectors.user;

import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Represents the MetaData of a User Metric that we want to collect from DataBase
 *
 * @see MetricType
 */
public interface UsersDatabaseMetricType extends DBMetricType {

    String USER_EXCLUDE="companyid<>'default' and userid<> 'system' and userid <> 'anonymous'";

}
