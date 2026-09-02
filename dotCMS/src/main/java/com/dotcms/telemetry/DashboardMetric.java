package com.dotcms.telemetry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a MetricType implementation for inclusion in the Usage API dashboard.
 * 
 * <p>Metrics annotated with this marker are automatically discovered by
 * {@link com.dotcms.telemetry.collectors.DashboardMetricsProvider} and included
 * in the {@code /v1/usage/summary} endpoint response. Without this annotation,
 * metrics are still available via the general telemetry API but not included
 * in the dashboard summary.</p>
 * 
 * <p><strong>Important:</strong> Dashboard metrics must also be annotated with
 * {@link MetricsProfile} to control when they are collected based on performance
 * profiles. <strong>All metrics require {@code @MetricsProfile} annotation.</strong>
 * Metrics without this annotation will be excluded from collection.</p>
 * 
 * <p><strong>Relationship with {@link MetricsProfile}:</strong></p>
 * <ul>
 *     <li>{@code @DashboardMetric} = "What to show" (display intent, category, priority)</li>
 *     <li>{@code @MetricsProfile} = "When to collect" (performance profile restrictions)</li>
 * </ul>
 * 
 * <p>Example:</p>
 * <pre>
 * {@code
 * @ApplicationScoped
 * @MetricsProfile({ProfileType.MINIMAL, ProfileType.STANDARD, ProfileType.FULL})  // Required for profile filtering
 * @DashboardMetric(category = "site", priority = 1)
 * public class TotalSitesDatabaseMetricType implements DBMetricType {
 *     // ...
 * }
 * }
 * </pre>
 * 
 * @see MetricsProfile
 * @see ProfileType
 * @see com.dotcms.telemetry.collectors.DashboardMetricsProvider
 * @see com.dotcms.rest.api.v1.usage.UsageResource
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DashboardMetric {
    
    /**
     * Category for grouping related metrics. Used by
     * {@link com.dotcms.telemetry.collectors.DashboardMetricsProvider#getDashboardMetricsByCategory(String)}
     * to filter metrics. Common categories: "content", "site", "user", "system".
     * 
     * @return the category name, or empty string if not categorized
     */
    String category() default "";
    
    /**
     * Display order priority. Metrics are sorted by this value (ascending) when
     * retrieved via {@link com.dotcms.telemetry.collectors.DashboardMetricsProvider#getDashboardMetrics()}.
     * Lower values appear first.
     * 
     * @return the priority value
     */
    int priority() default 0;
}

