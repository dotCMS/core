package com.dotcms.jobs.business.detector;

import com.dotmarketing.util.Config;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * Produces configuration for the abandoned job detector system using application properties. This
 * producer creates a singleton configuration object that determines the intervals for checking
 * abandoned jobs and the threshold for considering a job abandoned.
 * <p>
 * Configuration is read from the following properties:
 * <ul>
 *   <li>JOB_ABANDONMENT_DETECTION_INTERVAL_MINUTES: How often to check for abandoned jobs (default: 5)</li>
 *   <li>JOB_ABANDONMENT_THRESHOLD_MINUTES: How long before a non-updating job is considered abandoned (default: 30)</li>
 * </ul>
 */
@ApplicationScoped
public class AbandonedJobDetectorConfigProducer {

    /**
     * The default interval in minutes between checks for abandoned jobs.
     */
    static final int DEFAULT_JOB_ABANDONMENT_DETECTION_INTERVAL_MINUTES = Config.getIntProperty(
            "JOB_ABANDONMENT_DETECTION_INTERVAL_MINUTES", 5
    );

    /**
     * The default time in minutes after which an inactive job is considered abandoned.
     */
    static final int DEFAULT_JOB_ABANDONMENT_THRESHOLD_MINUTES = Config.getIntProperty(
            "JOB_ABANDONMENT_THRESHOLD_MINUTES", 30
    );

    /**
     * Produces a configuration object for the abandoned job detector.
     *
     * @return A new configuration object with the current property values
     */
    @ApplicationScoped
    @Produces
    public AbandonedJobDetectorConfig produceAbandonedJobDetectorConfig() {
        return new AbandonedJobDetectorConfig(
                DEFAULT_JOB_ABANDONMENT_DETECTION_INTERVAL_MINUTES,
                DEFAULT_JOB_ABANDONMENT_THRESHOLD_MINUTES
        );
    }

}
