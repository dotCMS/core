package com.dotcms.jobs.business.job;

import com.dotcms.jobs.business.processor.ProgressTracker;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value.Default;

/**
 * Abstract interface for an immutable Job class. This interface defines the structure for job
 * This interface defines the structure for job information in the job processing system and it is also implemented by the JsonView class.
 */
public interface JobContract {

    String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    String id();

    String queueName();

    JobState state();

    Optional<String> executionNode();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    Optional<LocalDateTime> createdAt();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    Optional<LocalDateTime> startedAt();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    Optional<LocalDateTime> updatedAt();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    Optional<LocalDateTime> completedAt();

    Optional<JobResult> result();

    Map<String, Object> parameters();

    @JsonIgnore
    Optional<ProgressTracker> progressTracker();

    @Default
    default int retryCount() {
        return 0;
    }

    @Default
    default float progress() {
        return 0.0f;
    }

}
