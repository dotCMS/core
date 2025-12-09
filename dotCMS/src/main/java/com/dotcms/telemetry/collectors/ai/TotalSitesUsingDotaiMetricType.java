package com.dotcms.telemetry.collectors.ai;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;

import javax.ws.rs.NotSupportedException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TotalSitesUsingDotaiMetricType implements MetricType {

    @Override
    public String getName() {
        return "TOTAL_SITES_USING_DOTAI";
    }

    @Override
    public String getDescription() {
        return "Total number of Sites using dotAI";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.AI;
    }

    @Override
    public Optional<MetricValue> getStat() throws DotDataException {
        final AppsAPI appsAPI = APILocator.getAppsAPI();
        final String key = "dotAI";
        try {
            final Optional<AppDescriptor> appDescriptorOptional = appsAPI.getAppDescriptor(key, APILocator.systemUser());
            if (appDescriptorOptional.isEmpty()) {
                throw new DoesNotExistException(String.format("No App was found for key '%s'", key));
            }
            final AppDescriptor appDescriptor = appDescriptorOptional.get();
            final Map<String,Set<String>> appKeysByHost = appsAPI.appKeysByHost();
            final Set<String> sitesWithConfigurations = appsAPI.filterSitesForAppKey(
                    appDescriptor.getKey(), appKeysByHost.keySet(), APILocator.systemUser());
            return Optional.of(new MetricValue(this.getMetric(), sitesWithConfigurations.size()));
        } catch (final DotSecurityException e) {
            final String errorMsg = String.format("An error occurred when retrieving the total " +
                    "Sites using dotAI: %s", ExceptionUtil.getErrorMessage(e));
            Logger.error(this, errorMsg, e);
            throw new DotDataException(errorMsg);
        }
    }

    @Override
    public Optional<Object> getValue() {
        throw new NotSupportedException();
    }

}
