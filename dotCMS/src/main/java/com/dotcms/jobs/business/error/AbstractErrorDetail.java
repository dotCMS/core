package com.dotcms.jobs.business.error;



import com.dotcms.jobs.business.job.AbstractJob;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import org.immutables.value.Value;

/**
 * Abstract interface for an immutable ErrorDetail class. This interface defines the structure for
 * detailed error information in the job processing system. The concrete implementation will be
 * generated as an immutable class named ErrorDetail.
 * <p>
 * This class is designed to be serialized and deserialized to/from JSON.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = ErrorDetail.class)
@JsonDeserialize(as = ErrorDetail.class)
public interface AbstractErrorDetail {

    /**
     * Returns the error message.
     *
     * @return A string containing the error message.
     */
    String message();

    /**
     * Returns the fully qualified name of the exception class.
     *
     * @return A string representing the exception class name.
     */
    String exceptionClass();

    /**
     * Returns the stack trace of the exception as a string.
     *
     * @return A string representation of the exception's stack trace.
     */
    String stackTrace();

    /**
     * Returns the timestamp when the error occurred.
     *
     * @return A LocalDateTime representing when the error was recorded.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AbstractJob.DATE_PATTERN)
    LocalDateTime timestamp();

    /**
     * Returns the processing stage where the error occurred.
     *
     * @return A string describing the processing stage (e.g., "Job Execution", "Retry Handling").
     */
    String processingStage();

}