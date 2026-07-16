package com.dotcms.telemetry;

/**
 * Profile types for telemetry metric collection.
 * 
 * <p>Profiles control which metrics are collected based on performance requirements.
 * Metrics are annotated with {@link MetricsProfile} to declare which profiles
 * they support.</p>
 * 
 * <ul>
 *     <li><b>MINIMAL</b>: 10-15 core metrics, &lt; 5 seconds total collection time</li>
 *     <li><b>STANDARD</b>: ~50 metrics, &lt; 15 seconds total collection time (future)</li>
 *     <li><b>FULL</b>: All 128 metrics, background collection only (future)</li>
 * </ul>
 * 
 * @see MetricsProfile
 */
public enum ProfileType {
    /**
     * Minimal profile with 10-15 core metrics.
     * Designed for fast collection (&lt; 5 seconds total).
     * Used for Usage Portlet dashboard.
     */
    MINIMAL,
    
    /**
     * Standard profile with ~50 metrics.
     * Designed for comprehensive collection (&lt; 15 seconds total).
     * Future implementation.
     */
    STANDARD,
    
    /**
     * Full profile with all 128 metrics.
     * Designed for background collection only.
     * Future implementation.
     */
    FULL
}


