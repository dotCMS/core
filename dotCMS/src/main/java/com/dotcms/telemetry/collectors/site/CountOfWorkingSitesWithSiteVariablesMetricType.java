package com.dotcms.telemetry.collectors.site;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

/**
 * Collects the count of stopped (working) Sites that have Site Variables.
 *
 * @author Jose Castro
 * @since Oct 4th, 2024
 */
@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
public class CountOfWorkingSitesWithSiteVariablesMetricType extends CountOfSitesWithSiteVariablesMetricType {

    @Override
    public PublishStatus getPublishStatus() {
        return PublishStatus.WORKING;
    }

}
