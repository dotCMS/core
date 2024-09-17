package com.dotcms.jobs.business.processor;

/**
 * A default implementation of the ProgressTracker interface. This class provides a simple mechanism
 * for tracking job progress.
 */
public class DefaultProgressTracker implements ProgressTracker {

    private volatile float progress = 0.0f;

    @Override
    public void updateProgress(final float progress) {
        if (progress < 0.0f || progress > 1.0f) {
            throw new IllegalArgumentException("Progress must be between 0.0 and 1.0, inclusive");
        }
        this.progress = progress;
    }

    @Override
    public float progress() {
        return progress;
    }
}