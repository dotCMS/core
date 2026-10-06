package com.dotcms.jobs.business.processor;

/**
 * A default implementation of the ProgressTracker interface. This class provides a simple mechanism
 * for tracking job progress.
 */
public class DefaultProgressTracker implements ProgressTracker {

    private volatile float progress = 0.0f;

    private volatile boolean heartbeatPending = false;

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

    @Override
    public void heartbeat() {
        this.heartbeatPending = true;
    }

    /**
     * Framework-internal: reports whether {@link #heartbeat()} was called since the last time this
     * was checked, and clears the flag. Not part of the {@link ProgressTracker} contract processors
     * see — consumed only by the job-queue framework's own progress-poller, which is the sole
     * reader/writer of this field and therefore does not need this to be more than best-effort
     * visible under {@code volatile}.
     *
     * @return true if {@link #heartbeat()} was called since the last call to this method
     */
    public boolean consumeHeartbeat() {
        final boolean pending = this.heartbeatPending;
        this.heartbeatPending = false;
        return pending;
    }
}