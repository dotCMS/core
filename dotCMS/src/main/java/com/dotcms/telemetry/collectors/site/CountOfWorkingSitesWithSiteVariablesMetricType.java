package com.dotcms.telemetry.collectors.site;

/**
 * Collects the count of stopped (working) Sites that have Site Variables.
 *
 * @author Jose Castro
 * @since Oct 4th, 2024
 */
public class CountOfWorkingSitesWithSiteVariablesMetricType extends CountOfSitesWithSiteVariablesMetricType {

    @Override
    public PublishStatus getPublishStatus() {
        return PublishStatus.WORKING;
    }

}
