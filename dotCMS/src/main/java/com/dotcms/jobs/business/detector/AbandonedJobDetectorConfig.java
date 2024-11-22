package com.dotcms.jobs.business.detector;

/**
 * Configuration settings for the abandoned job detection system. This class holds the timing
 * parameters that control how and when jobs are determined to be abandoned.
 */
public class AbandonedJobDetectorConfig {

    private final long detectionIntervalMinutes;
    private final long abandonmentThresholdMinutes;

    // Required for CDI proxy
    protected AbandonedJobDetectorConfig() {
        this.detectionIntervalMinutes = 0;
        this.abandonmentThresholdMinutes = 0;
    }

    /**
     * Constructs a new configuration with the specified timing parameters.
     *
     * @param detectionIntervalMinutes    How frequently to check for abandoned jobs, in minutes
     * @param abandonmentThresholdMinutes How long a job must be inactive before being considered
     *                                    abandoned, in minutes
     */
    public AbandonedJobDetectorConfig(
            long detectionIntervalMinutes,
            long abandonmentThresholdMinutes) {
        this.detectionIntervalMinutes = detectionIntervalMinutes;
        this.abandonmentThresholdMinutes = abandonmentThresholdMinutes;
    }

    /**
     * Gets the interval between abandoned job detection runs.
     *
     * @return The detection interval in minutes
     */
    public long getDetectionIntervalMinutes() {
        return detectionIntervalMinutes;
    }

    /**
     * Gets the time threshold after which an inactive job is considered abandoned.
     *
     * @return The abandonment threshold in minutes
     */
    public long getAbandonmentThresholdMinutes() {
        return abandonmentThresholdMinutes;
    }

}
