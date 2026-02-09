package com.dotcms.telemetry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which {@link ProfileType} profiles a metric supports.
 * 
 * <p><strong>Required:</strong> All metrics must be annotated with this annotation.
 * Metrics without this annotation will be excluded from collection.</p>
 * 
 * <p>Metrics annotated with this annotation are filtered by the active profile
 * (configured via {@code telemetry.default.profile} or {@code telemetry.cron.profile}).
 * Only metrics that support the active profile will be collected.</p>
 * 
 * <p><strong>Relationship with {@link DashboardMetric}:</strong></p>
 * <ul>
 *     <li>{@code @DashboardMetric} = "What to show" (display intent, category, priority)</li>
 *     <li>{@code @MetricsProfile} = "When to collect" (performance profile restrictions)</li>
 * </ul>
 * 
 * <p>These annotations work together in a two-stage filtering process:</p>
 * <ol>
 *     <li>First, metrics are filtered by {@code @DashboardMetric} (for dashboard display)</li>
 *     <li>Then, metrics are filtered by {@code @MetricsProfile} (for performance control)</li>
 * </ol>
 * 
 * <p>Example:</p>
 * <pre>
 * {@code
 * @ApplicationScoped
 * @MetricsProfile({ProfileType.MINIMAL, ProfileType.STANDARD, ProfileType.FULL})  // Performance control
 * @DashboardMetric(category = "site", priority = 1)                                // Display intent
 * public class TotalSitesDatabaseMetricType implements DBMetricType {
 *     // ...
 * }
 * }
 * </pre>
 * 
 * @see ProfileType
 * @see DashboardMetric
 * @see com.dotcms.telemetry.collectors.ProfileFilter
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MetricsProfile {
    /**
     * Array of profile types this metric supports.
     * 
     * @return array of supported profile types
     */
    ProfileType[] value();
}

