package com.dotcms.telemetry.collectors.site;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the count of stopped (working) Sites that have Site Variables.
 *
 * @author Jose Castro
 * @since Oct 4th, 2024
 */
@ApplicationScoped
public class CountOfWorkingSitesWithSiteVariablesMetricType extends CountOfSitesWithSiteVariablesMetricType {

    @Override
    public PublishStatus getPublishStatus() {
        return PublishStatus.WORKING;
    }

}
