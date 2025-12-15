package com.dotcms.telemetry.collectors;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;
import com.dotcms.telemetry.cache.MetricCacheConfig;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discovers and provides dashboard-available metrics via CDI.
 * 
 * <p>Used by {@link com.dotcms.rest.api.v1.usage.UsageResource} to collect
 * metrics for the dashboard endpoint. Only metrics annotated with
 * {@link DashboardMetric} are included.</p>
 * 
 * <p>Discovery is automatic - all {@code @ApplicationScoped MetricType} beans
 * are scanned at runtime. Metrics are sorted by {@link DashboardMetric#priority()}
 * before being returned.</p>
 * 
 * @see DashboardMetric
 * @see com.dotcms.rest.api.v1.usage.UsageResource
 */
@ApplicationScoped
public class DashboardMetricsProvider {
    
    @Inject
    private BeanManager beanManager;
    
    @Inject
    private Instance<MetricType> allMetrics;
    
    @Inject
    private MetricCacheConfig config;
    
    /**
     * Returns all metrics annotated with {@link DashboardMetric}, sorted by priority.
     * Uses the default profile from configuration.
     * 
     * <p>Uses CDI's {@link BeanManager} to query for beans with the annotation,
     * which is the proper CDI way to discover annotated beans. This avoids
     * issues with proxy classes and leverages CDI's built-in capabilities.</p>
     * 
     * <p>This method is called by {@link com.dotcms.rest.api.v1.usage.UsageResource}
     * to collect metrics for the dashboard. The returned list is sorted by
     * {@link DashboardMetric#priority()} in ascending order.</p>
     * 
     * @return list of dashboard-available metrics, sorted by priority (lowest first)
     */
    public List<MetricType> getDashboardMetrics() {
        return getDashboardMetrics(null);
    }
    
    /**
     * Returns all metrics annotated with {@link DashboardMetric}, filtered by the specified profile.
     * If profile is null, uses the default profile from configuration.
     * 
     * <p>Uses CDI's {@link BeanManager} to query for beans with the annotation,
     * which is the proper CDI way to discover annotated beans. This avoids
     * issues with proxy classes and leverages CDI's built-in capabilities.</p>
     * 
     * <p>This method is called by {@link com.dotcms.rest.api.v1.usage.UsageResource}
     * to collect metrics for the dashboard. The returned list is sorted by
     * {@link DashboardMetric#priority()} in ascending order.</p>
     * 
     * @param profileOverride optional profile to use instead of the configured default (null = use default)
     * @return list of dashboard-available metrics, sorted by priority (lowest first)
     */
    public List<MetricType> getDashboardMetrics(final ProfileType profileOverride) {
        Logger.debug(this, "Starting dashboard metrics discovery via CDI BeanManager");

        // Get active profile - use override if provided, otherwise use configuration default
        final ProfileType activeProfile = profileOverride != null ? profileOverride : config.getActiveProfile();
        Logger.debug(this, () -> String.format("Filtering dashboard metrics for profile: %s%s", 
                activeProfile, profileOverride != null ? " (overridden)" : " (from config)"));

        try {
            // Use BeanManager to get all MetricType beans - this gives us Bean objects
            // which contain the actual class information (not proxies)
            final Set<Bean<?>> allBeans = beanManager.getBeans(MetricType.class, Any.Literal.INSTANCE);

            Logger.debug(this, () -> String.format("BeanManager found %d MetricType beans", allBeans.size()));
            
            // Use a list that stores both the metric and its annotation for efficient sorting
            final List<MetricWithAnnotation> dashboardMetricsWithAnnotation = new ArrayList<>();
            
            // Iterate through Bean objects to check annotations on actual classes
            for (Bean<?> bean : allBeans) {
                final Class<?> beanClass = bean.getBeanClass();
                
                // Check if the bean class has @DashboardMetric annotation
                // bean.getBeanClass() returns the actual class, not the proxy
                if (beanClass.isAnnotationPresent(DashboardMetric.class)) {
                    final DashboardMetric annotation = beanClass.getAnnotation(DashboardMetric.class);
                    
                    // Validate: All metrics must have @MetricsProfile annotation
                    if (!beanClass.isAnnotationPresent(MetricsProfile.class)) {
                        Logger.error(this, String.format(
                            "Dashboard metric %s has @DashboardMetric but missing required @MetricsProfile annotation. " +
                            "This metric will be excluded from collection. " +
                            "Please add @MetricsProfile annotation to the metric class.",
                            beanClass.getName()));
                    }
                    
                    // Get the actual bean instance from CDI
                    final MetricType metric = (MetricType) beanManager.getReference(
                            bean, MetricType.class, beanManager.createCreationalContext(bean));
                    
                    // Apply profile filter
                    if (ProfileFilter.matches(metric, activeProfile)) {
                        dashboardMetricsWithAnnotation.add(new MetricWithAnnotation(metric, annotation));
                        Logger.debug(this, () -> String.format("Found dashboard metric: %s (category: %s, priority: %d)",
                                beanClass.getName(), annotation.category(), annotation.priority()));
                    } else {
                        Logger.debug(this, () -> String.format("Excluding dashboard metric %s (does not match profile %s)",
                                beanClass.getName(), activeProfile));
                    }
                }
            }

            Logger.debug(this, () -> String.format("CDI discovered %d total MetricType beans, %d annotated with @DashboardMetric, %d match profile %s",
                    allBeans.size(), dashboardMetricsWithAnnotation.size(), dashboardMetricsWithAnnotation.size(), activeProfile));
            
            // Sort by priority (from @DashboardMetric annotation)
            dashboardMetricsWithAnnotation.sort(Comparator.comparingInt(m -> m.annotation.priority()));
            
            // Extract just the metrics
            return dashboardMetricsWithAnnotation.stream()
                    .map(m -> m.metric)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            Logger.error(this, "Error discovering dashboard metrics via BeanManager", e);
            // Fallback to Instance-based discovery if BeanManager fails
            return getDashboardMetricsFallback(activeProfile);
        }
    }
    
    /**
     * Fallback method using Instance injection if BeanManager approach fails.
     * This method handles proxy classes by checking superclasses.
     * 
     * @param activeProfile the profile to filter metrics by
     */
    private List<MetricType> getDashboardMetricsFallback(final ProfileType activeProfile) {
        Logger.warn(this, "Falling back to Instance-based discovery");
        final List<MetricType> dashboardMetrics = new ArrayList<>();
        
        for (MetricType metric : allMetrics) {
            final Class<?> actualClass = getBeanClass(metric);
            if (actualClass.isAnnotationPresent(DashboardMetric.class)) {
                // Validate: All metrics must have @MetricsProfile annotation
                if (!actualClass.isAnnotationPresent(MetricsProfile.class)) {
                    Logger.error(this, String.format(
                        "Dashboard metric %s has @DashboardMetric but missing required @MetricsProfile annotation. " +
                        "This metric will be excluded from collection. " +
                        "Please add @MetricsProfile annotation to the metric class.",
                        actualClass.getName()));
                }
                
                // Apply profile filter
                if (ProfileFilter.matches(metric, activeProfile)) {
                    dashboardMetrics.add(metric);
                }
            }
        }
        
        dashboardMetrics.sort(Comparator.comparingInt(metric -> {
            final Class<?> metricClass = getBeanClass(metric);
            final DashboardMetric annotation = metricClass.getAnnotation(DashboardMetric.class);
            return annotation != null ? annotation.priority() : 0;
        }));
        
        return dashboardMetrics;
    }
    
    /**
     * Gets the actual bean class, handling CDI proxy classes.
     * For BeanManager-discovered beans, this should not be needed, but kept
     * as a fallback for Instance-based discovery.
     */
    private Class<?> getBeanClass(final MetricType metric) {
        Class<?> clazz = metric.getClass();
        
        // CDI proxies often have names like "TotalSitesDatabaseMetricType$Proxy$..." 
        // or extend the actual class. Check superclass if this looks like a proxy.
        if (clazz.getName().contains("$Proxy") || clazz.getName().contains("$$")) {
            Class<?> superclass = clazz.getSuperclass();
            // If superclass is not Object and is assignable to MetricType, use it
            if (superclass != null && !superclass.equals(Object.class) && MetricType.class.isAssignableFrom(superclass)) {
                return superclass;
            }
        }
        
        return clazz;
    }
    
    /**
     * Returns dashboard metrics filtered by {@link DashboardMetric#category()}.
     * Uses the default profile from configuration.
     * 
     * @param category the category to filter by (case-insensitive)
     * @return list of metrics in the specified category, sorted by priority
     */
    public List<MetricType> getDashboardMetricsByCategory(final String category) {
        return getDashboardMetricsByCategory(category, null);
    }
    
    /**
     * Returns dashboard metrics filtered by {@link DashboardMetric#category()}.
     * 
     * @param category the category to filter by (case-insensitive)
     * @param profileOverride optional profile to use instead of the configured default (null = use default)
     * @return list of metrics in the specified category, sorted by priority
     */
    public List<MetricType> getDashboardMetricsByCategory(final String category, final ProfileType profileOverride) {
        return getDashboardMetrics(profileOverride).stream()
                .filter(metric -> {
                    final String metricCategory = getCategory(metric);
                    return metricCategory != null && metricCategory.equalsIgnoreCase(category);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Finds a dashboard metric by its {@link MetricType#getName()} value.
     * 
     * @param metricName the metric name to find
     * @return the metric if found, null otherwise
     */
    public MetricType getDashboardMetricByName(final String metricName) {
        return getDashboardMetrics().stream()
                .filter(metric -> metricName.equals(metric.getName()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets the category for a metric from its @DashboardMetric annotation.
     * 
     * @param metric the metric instance
     * @return the category name, or null if not specified
     */
    public String getCategory(final MetricType metric) {
        final Class<?> metricClass = getBeanClass(metric);
        final DashboardMetric annotation = metricClass.getAnnotation(DashboardMetric.class);
        if (annotation != null) {
            final String category = annotation.category();
            return category != null && !category.isEmpty() ? category : null;
        }
        return null;
    }
    
    /**
     * Helper class to hold a metric and its annotation together for efficient sorting.
     */
    private static class MetricWithAnnotation {
        final MetricType metric;
        final DashboardMetric annotation;
        
        MetricWithAnnotation(final MetricType metric, final DashboardMetric annotation) {
            this.metric = metric;
            this.annotation = annotation;
        }
    }
}

