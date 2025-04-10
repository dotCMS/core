package com.dotcms.jobs.business.job;

import com.dotcms.jobs.business.processor.ProgressTracker;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value.Default;

/**
 * This interface defines the structure for job information in the job processing system and it is 
 * also implemented by the JsonView class.
 */
public interface JobContract {

    String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @JsonInclude(Include.NON_EMPTY)
    String id();

    String queueName();

    JobState state();

    @JsonInclude(Include.NON_EMPTY)
    Optional<String> executionNode();

    @JsonInclude(Include.NON_EMPTY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    Optional<LocalDateTime> createdAt();

    @JsonInclude(Include.NON_EMPTY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    Optional<LocalDateTime> startedAt();

    @JsonInclude(Include.NON_EMPTY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    Optional<LocalDateTime> updatedAt();

    @JsonInclude(Include.NON_EMPTY)
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
