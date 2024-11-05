package com.dotcms.telemetry.collectors.site;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collects the count of Sites that have Site Variables.
 *
 * @author Jose Castro
 * @since Oct 4th, 2024
 */
public abstract class CountOfSitesWithSiteVariablesMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "WITH_" + getPublishStatus().name() + "_SITE_VARIABLES";
    }

    @Override
    public String getDescription() {
        return "Count of " + getPublishStatus().getName() + " Sites that have Site Variables";
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
        return "SELECT COUNT(identifier) AS value FROM contentlet_version_info WHERE deleted IS FALSE AND "
                + getPublishStatus().getCondition() + " AND identifier IN (SELECT DISTINCT " +
                "host_id FROM host_variable)";
    }

    /**
     * Allows you to specify whether you want to collect Metrics from a started or stopped Site.
     *
     * @return The {@link PublishStatus} used to generate the metrics.
     */
    public abstract PublishStatus getPublishStatus();

    /**
     * Determines the publish status of a given Site: Started (live) or Stopped (working).
     */
    public enum PublishStatus {
        WORKING("Working", "(working_inode <> live_inode OR live_inode IS NULL)"),
        LIVE("Live", "working_inode = live_inode");

        private final String name;
        private final String condition;

        PublishStatus(final String value, final String condition) {
            this.name = value;
            this.condition = condition;
        }

        public String getName() {
            return name;
        }

        public String getCondition() {
            return condition;
        }

    }

}
