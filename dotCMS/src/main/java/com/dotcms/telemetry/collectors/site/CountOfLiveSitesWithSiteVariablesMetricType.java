package com.dotcms.telemetry.collectors.site;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the count of started (live) Sites that have Site Variables.
 *
 * @author Jose Castro
 * @since Oct 4th, 2024
 */
@ApplicationScoped
public class CountOfLiveSitesWithSiteVariablesMetricType extends CountOfSitesWithSiteVariablesMetricType {

    @Override
    public PublishStatus getPublishStatus() {
        return PublishStatus.LIVE;
    }

}
