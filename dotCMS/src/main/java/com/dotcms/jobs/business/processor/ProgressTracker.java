package com.dotcms.jobs.business.processor;

/**
 * Interface for tracking the progress of a job. Implementations of this interface should provide
 * mechanisms to update and retrieve the current progress of a job.
 */
public interface ProgressTracker {

    /**
     * Updates the progress of the job.
     *
     * @param progress A float value between 0.0 and 1.0, inclusive, representing the job's
     *                 progress.
     * @throws IllegalArgumentException if progress is not between 0.0 and 1.0, inclusive.
     */
    void updateProgress(float progress);

    /**
     * Retrieves the current progress of the job.
     *
     * @return A float value between 0.0 and 1.0, inclusive, representing the job's current
     * progress.
     */
    float progress();

    /**
     * Signals that the job is still actively working, independent of {@link #progress()}.
     * <p>
     * The framework's own progress poller only refreshes a job's {@code updated_at} timestamp when
     * the reported progress value strictly increases, so a processor whose progress can only be
     * reported at a coarse granularity — one unit of work that does not expose any finer-grained
     * completion signal while it runs, such as one recursive folder delete — can go past the
     * abandonment threshold with a perfectly healthy job, purely because the number has nowhere to
     * move. Calling this during such a unit of work keeps {@code updated_at} fresh without
     * inventing a progress value more precise than the processor actually has (#37063, spec
     * FR-024a).
     * <p>
     * Unlike {@link #updateProgress(float)}, a heartbeat never changes {@link #progress()} and never
     * causes a progress-changed notification to reach a job watcher — it exists purely to keep the
     * job from being mistaken for abandoned, not to report progress.
     * <p>
     * Default no-op so existing implementations of this interface — including test doubles —
     * continue to compile unchanged; {@link DefaultProgressTracker}, the one the framework actually
     * hands to processors, overrides it with real behavior.
     */
    default void heartbeat() {
        // no-op by default; see DefaultProgressTracker for the real implementation
    }
}