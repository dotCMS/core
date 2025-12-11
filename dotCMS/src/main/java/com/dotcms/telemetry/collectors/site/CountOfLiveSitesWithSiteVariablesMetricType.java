package com.dotcms.telemetry.collectors.site;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

/**
 * Collects the count of started (live) Sites that have Site Variables.
 *
 * @author Jose Castro
 * @since Oct 4th, 2024
 */
@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
public class CountOfLiveSitesWithSiteVariablesMetricType extends CountOfSitesWithSiteVariablesMetricType {

    @Override
    public PublishStatus getPublishStatus() {
        return PublishStatus.LIVE;
    }

    @Override
    public String getDisplayLabel() {
        return "Live Sites with Site Variables";
    }

}
