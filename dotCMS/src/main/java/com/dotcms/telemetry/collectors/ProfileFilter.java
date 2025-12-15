package com.dotcms.telemetry.collectors;

import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;
import com.dotmarketing.util.Logger;

/**
 * Utility class for filtering metrics by profile type.
 * 
 * <p>Checks if a metric supports the active profile by examining its
 * {@link MetricsProfile} annotation. <strong>All metrics must have this annotation.</strong>
 * Metrics without the annotation will be excluded from collection.</p>
 * 
 * <p>Handles CDI proxy classes by checking superclasses when necessary.</p>
 * 
 * @see MetricsProfile
 * @see ProfileType
 */
public class ProfileFilter {
    
    /**
     * Checks if a metric matches the active profile.
     * 
     * <p><strong>Required:</strong> The metric must have a {@link MetricsProfile} annotation.
     * Metrics without this annotation will be excluded.</p>
     * 
     * @param metric the metric to check
     * @param activeProfile the active profile to match against
     * @return true if the metric should be included, false otherwise
     */
    public static boolean matches(final MetricType metric, final ProfileType activeProfile) {
        final Class<?> metricClass = getMetricClass(metric);
        final MetricsProfile annotation = metricClass.getAnnotation(MetricsProfile.class);
        
        // If no annotation, exclude (annotation is required)
        if (annotation == null) {
            Logger.warn(ProfileFilter.class, 
                String.format("Metric %s is missing required @MetricsProfile annotation. " +
                    "This metric will be excluded from collection. " +
                    "Please add @MetricsProfile annotation to the metric class.",
                    metricClass.getName()));
            return false;
        }
        
        // Check if metric's profiles include active profile
        final ProfileType[] profiles = annotation.value();
        for (final ProfileType profile : profiles) {
            if (profile == activeProfile) {
                Logger.debug(ProfileFilter.class, () -> 
                    String.format("Metric %s matches profile %s", metricClass.getName(), activeProfile));
                return true;
            }
        }
        
        Logger.debug(ProfileFilter.class, () -> 
            String.format("Metric %s does not support profile %s, excluding", 
                metricClass.getName(), activeProfile));
        return false;
    }
    
    /**
     * Gets the actual bean class, handling CDI proxy classes.
     * 
     * <p>CDI proxies often have names like "ClassName$Proxy$..." or extend
     * the actual class. This method checks the superclass if the class looks
     * like a proxy.</p>
     * 
     * @param metric the metric instance
     * @return the actual metric class (not the proxy)
     */
    private static Class<?> getMetricClass(final MetricType metric) {
        Class<?> clazz = metric.getClass();
        
        // Handle CDI proxies
        if (clazz.getName().contains("$Proxy") || clazz.getName().contains("$$")) {
            final Class<?> superclass = clazz.getSuperclass();
            // If superclass is not Object and is assignable to MetricType, use it
            if (superclass != null && !superclass.equals(Object.class) && MetricType.class.isAssignableFrom(superclass)) {
                return superclass;
            }
        }
        
        return clazz;
    }
}


