package com.dotcms.health.providers;

import com.dotcms.health.api.HealthCheck;
import com.dotcms.health.api.HealthCheckProvider;
import com.dotcms.health.checks.cdi.CacheHealthCheck;
import com.dotcms.health.checks.cdi.DatabaseHealthCheck;
import com.dotcms.health.checks.cdi.ElasticsearchHealthCheck;
import java.util.Arrays;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;

/**
 * Health check provider that contributes CDI-based dependency health checks.
 * These checks monitor external dependencies like database and cache.
 * 
 * Note: Creates instances directly to avoid CDI qualifier complexity.
 * The health checks themselves can still use CDI for their dependencies if needed.
 */
@ApplicationScoped
public class CoreHealthCheckProvider implements HealthCheckProvider {

    @Override
    public List<HealthCheck> getHealthChecks() {
        return Arrays.asList(
            new DatabaseHealthCheck(),
            new CacheHealthCheck(),
            new ElasticsearchHealthCheck()
            // Additional dependency health checks can be added here
        );
    }

    @Override
    public String getProviderName() {
        return "core-dependencies";
    }
}