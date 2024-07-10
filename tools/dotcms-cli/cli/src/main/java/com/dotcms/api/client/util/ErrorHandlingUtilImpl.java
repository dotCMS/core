package com.dotcms.api.client.util;

import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.pull.exception.PullException;
import com.dotcms.api.client.push.exception.PushException;
import com.dotcms.cli.common.OutputOptionMixin;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import picocli.CommandLine.ExitCode;

/**
 * This class provides an implementation of the ErrorHandlingUtil interface. It provides methods to
 * handle exceptions that occur during the push and pull processes. It also provides methods to
 * handle fail-fast exceptions during these processes and map exceptions to specific types.
 */
@ApplicationScoped
public class ErrorHandlingUtilImpl implements ErrorHandlingUtil {

    private enum OperationType {
        PUSH, PULL
    }

    /**
     * Handles exceptions that occur during the push process.
     *
     * @param errors A list of exceptions that occurred during the push process.
     * @param output An instance of OutputOptionMixin to handle output options.
     * @return The exit code indicating the status of the process.
     */
    public int handlePushExceptions(
            final List<Exception> errors, final OutputOptionMixin output) {
        return handleExceptions(errors, output, OperationType.PUSH);
    }

    /**
     * Handles exceptions that occur during the pull process.
     *
     * @param errors A list of exceptions that occurred during the pull process.
     * @param output An instance of OutputOptionMixin to handle output options.
     * @return The exit code indicating the status of the process.
     */
    public int handlePullExceptions(
            final List<Exception> errors, final OutputOptionMixin output) {
        return handleExceptions(errors, output, OperationType.PULL);
    }

    /**
     * Handles exceptions and returns the exit code indicating the status of the process.
     *
     * @param errors  A list of exceptions that occurred during the process.
     * @param output  An instance of OutputOptionMixin to handle output options.
     * @param forType The type of operation (PULL or PUSH).
     * @return The exit code indicating the status of the process.
     */
    private int handleExceptions(
            final List<Exception> errors, final OutputOptionMixin output, OperationType forType) {

        int exitCode = ExitCode.OK;
        if (!errors.isEmpty()) {
            output.info(
                    String.format(
                            "%n%nFound [@|bold,red %s|@] errors during the %s process:",
                            errors.size(),
                            OperationType.PULL.equals(forType) ? "pull" : "push"
                    )
            );
            final ListIterator<Exception> iterator = errors.listIterator();
            //Let's save the first error for the exit code we assume it is the most relevant one
            boolean firstErrorCodeSet = false;
            while (iterator.hasNext()) {

                var exception = iterator.next();

                var isLast = !iterator.hasNext();

                final var errorCode = output.handleCommandException(
                        exception,
                        isLast
                );

                if (!firstErrorCodeSet) {
                    firstErrorCodeSet = true;
                    exitCode = errorCode;
                }
            }
        }
        return exitCode;
    }

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
    public Optional<RuntimeException> handlePushFailFastException(int retryAttempts,
            int maxRetryAttempts, OutputOptionMixin output, Throwable cause) {
        return handleFailFastException(retryAttempts, maxRetryAttempts, output, cause,
                OperationType.PUSH);
    }

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
    public Optional<RuntimeException> handlePullFailFastException(int retryAttempts,
            int maxRetryAttempts, OutputOptionMixin output, Throwable cause) {
        return handleFailFastException(retryAttempts, maxRetryAttempts, output, cause,
                OperationType.PULL);
    }

    /**
     * Handles the fail-fast exception that occurs during a process.
     *
     * @param retryAttempts    The number of retry attempts made.
     * @param maxRetryAttempts The maximum number of retry attempts allowed.
     * @param output           An instance of OutputOptionMixin to handle output options.
     * @param cause            The cause of the exception.
     * @param forType          The type of operation (PULL or PUSH).
     * @return An Optional RuntimeException which is empty if the process can be retried, or
     * contains the exception if the maximum retry attempts have been reached.
     */
    private Optional<RuntimeException> handleFailFastException(int retryAttempts,
            int maxRetryAttempts, OutputOptionMixin output, Throwable cause,
            OperationType forType) {

        if (retryAttempts + 1 <= maxRetryAttempts) {
            output.info(
                    String.format(
                            "%n%nFound errors during the %s process:",
                            OperationType.PULL.equals(forType) ? "pull" : "push"
                    )
            );
            output.error(cause.getMessage());

            return Optional.empty();
        } else {

            final RuntimeException exception = mapException(cause, forType);
            return Optional.of(exception);
        }
    }

    /**
     * Maps exceptions to specific types based on the cause of the exception during the push
     * process.
     *
     * @param cause The cause of the exception.
     * @return A RuntimeException of the appropriate type.
     */
    public RuntimeException mapPushException(Throwable cause) {
        return mapException(cause, OperationType.PUSH);
    }

    /**
     * Maps exceptions to specific types based on the cause of the exception during the pull
     * process.
     *
     * @param cause The cause of the exception.
     * @return A RuntimeException of the appropriate type.
     */
    public RuntimeException mapPullException(Throwable cause) {
        return mapException(cause, OperationType.PULL);
    }

    /**
     * Maps exceptions to specific types based on the cause of the exception and the operation
     * type.
     *
     * @param cause   The cause of the exception to be mapped.
     * @param forType The type of operation (PUSH or PULL).
     * @return A RuntimeException of the appropriate type.
     */
    private RuntimeException mapException(Throwable cause, OperationType forType) {

        final RuntimeException exception;

        if (cause instanceof PushException) {
            exception = (PushException) cause;
        } else if (cause instanceof PullException) {
            exception = (PullException) cause;
        } else if (cause instanceof TraversalTaskException) {
            exception = (TraversalTaskException) cause;
        } else {
            if (OperationType.PULL.equals(forType)) {
                exception = new PullException(cause.getMessage(), cause);
            } else {
                exception = new PushException(cause.getMessage(), cause);
            }
        }

        return exception;
    }

}
