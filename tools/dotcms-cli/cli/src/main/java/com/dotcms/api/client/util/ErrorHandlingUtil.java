package com.dotcms.api.client.util;

import com.dotcms.cli.common.OutputOptionMixin;
import java.util.List;
import java.util.Optional;

/**
 * This interface provides methods to handle exceptions that occur during the push and pull
 * processes. It also provides methods to handle fail-fast exceptions during these processes and map
 * exceptions to specific types.
 */
public interface ErrorHandlingUtil {

    /**
     * Handles exceptions that occur during the push process.
     *
     * @param errors A list of exceptions that occurred during the push process.
     * @param output An instance of OutputOptionMixin to handle output options.
     * @return The exit code indicating the status of the process.
     */
    int handlePushExceptions(List<Exception> errors, OutputOptionMixin output);

    /**
     * Handles exceptions that occur during the pull process.
     *
     * @param errors A list of exceptions that occurred during the pull process.
     * @param output An instance of OutputOptionMixin to handle output options.
     * @return The exit code indicating the status of the process.
     */
    int handlePullExceptions(List<Exception> errors, OutputOptionMixin output);

    /**
     * Handles fail-fast exceptions that occur during the push process.
     *
     * @param retryAttempts    The number of retry attempts made.
     * @param maxRetryAttempts The maximum number of retry attempts allowed.
     * @param output           An instance of OutputOptionMixin to handle output options.
     * @param cause            The cause of the exception.
     * @return An Optional RuntimeException which is empty if the process can be retried, or
     * contains the exception if the maximum retry attempts have been reached.
     */
    Optional<RuntimeException> handlePushFailFastException(int retryAttempts,
            int maxRetryAttempts, OutputOptionMixin output, Throwable cause);

    /**
     * Handles fail-fast exceptions that occur during the pull process.
     *
     * @param retryAttempts    The number of retry attempts made.
     * @param maxRetryAttempts The maximum number of retry attempts allowed.
     * @param output           An instance of OutputOptionMixin to handle output options.
     * @param cause            The cause of the exception.
     * @return An Optional RuntimeException which is empty if the process can be retried, or
     * contains the exception if the maximum retry attempts have been reached.
     */
    Optional<RuntimeException> handlePullFailFastException(int retryAttempts,
            int maxRetryAttempts, OutputOptionMixin output, Throwable cause);

    /**
     * Maps exceptions to specific types based on the cause of the exception during the push
     * process.
     *
     * @param cause The cause of the exception.
     * @return A RuntimeException of the appropriate type.
     */
    RuntimeException mapPushException(Throwable cause);

    /**
     * Maps exceptions to specific types based on the cause of the exception during the pull
     * process.
     *
     * @param cause The cause of the exception.
     * @return A RuntimeException of the appropriate type.
     */
    RuntimeException mapPullException(Throwable cause);

}
