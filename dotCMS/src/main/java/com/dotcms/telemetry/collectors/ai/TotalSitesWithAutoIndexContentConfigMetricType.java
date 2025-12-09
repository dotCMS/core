package com.dotcms.telemetry.collectors.ai;

import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;

import javax.ws.rs.NotSupportedException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TotalSitesWithAutoIndexContentConfigMetricType implements MetricType {

    @Override
    public String getName() {
        return "TOTAL_SITES_WITH_AUTO_INDEX_CONTENT_CONFIG";
    }

    @Override
    public String getDescription() {
        return "Total number of Sites with Auto Index Content Configuration set in its configuration";
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
                throw new DoesNotExistException(String.format("No App was found for key: '%s'", key));
            }
            final Map<String, Set<String>> appKeysByHost = appsAPI.appKeysByHost();
            int counter = 0;
            for (final String siteId : appKeysByHost.keySet()) {
                final Host site = APILocator.getHostAPI().find(siteId, APILocator.systemUser(), false);
                final Optional<AppSecrets> secrets = appsAPI.getSecrets(key, site, APILocator.systemUser());
                if (secrets.isPresent()) {
                    final Map<String, Secret> secretsMap = secrets.get().getSecrets();
                    if (UtilMethods.isSet(secretsMap.getOrDefault("listenerIndexer", null))) {
                        final Secret secret = secretsMap.get("listenerIndexer");
                        if (null != secret.getValue() && UtilMethods.isSet(new String(secret.getValue()))) {
                            counter++;
                        }
                    }
                }
            }
            return Optional.of(new MetricValue(this.getMetric(), counter));
        } catch (final DotSecurityException e) {
            throw new DotDataException(e);
        }
    }

    @Override
    public Optional<Object> getValue() {
        throw new NotSupportedException();
    }

}
