package com.dotcms.jobs.business.error;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.util.Arrays;
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
     * Returns the timestamp when the error occurred.
     *
     * @return A LocalDateTime representing when the error was recorded.
     */
    LocalDateTime timestamp();

    /**
     * Returns the processing stage where the error occurred.
     *
     * @return A string describing the processing stage (e.g., "Job Execution", "Retry Handling").
     */
    String processingStage();

    /**
     * Returns the original Throwable object that caused the error.
     *
     * @return The Throwable object, or null if no exception is available.
     */
    Throwable exception();

    /**
     * Generates and returns the stack trace of the exception as a string. This is a derived value
     * and will be computed only when accessed.
     *
     * @return A string representation of the exception's stack trace, or null if no exception is
     * present.
     */
    @Value.Derived
    default String stackTrace() {
        Throwable ex = exception();
        if (ex != null) {
            return Arrays.stream(ex.getStackTrace())
                    .map(StackTraceElement::toString)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        }
        return null;
    }

    /**
     * Returns a truncated version of the stack trace.
     *
     * @param maxLines The maximum number of lines to include in the truncated stack trace.
     * @return A string containing the truncated stacktrace, or null if no exception is present.
     */
    @Value.Derived
    default String truncatedStackTrace(int maxLines) {
        String fullTrace = stackTrace();
        if (fullTrace == null) {
            return null;
        }
        String[] lines = fullTrace.split("\n");
        return Arrays.stream(lines)
                .limit(maxLines)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

}