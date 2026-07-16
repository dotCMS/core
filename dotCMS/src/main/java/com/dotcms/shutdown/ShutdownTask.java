package com.dotcms.shutdown;

/**
 * Represents a single, named shutdown operation that can be discovered and executed by the {@link ShutdownCoordinator}.
 * Implementations of this interface should be CDI beans (e.g., {@code @ApplicationScoped}) and be annotated with
 * {@link ShutdownOrder} to control the execution sequence.
 */
public interface ShutdownTask {

    /**
     * A descriptive name for the shutdown task, used for logging.
     * @return The name of the task.
     */
    String getName();

    /**
     * The logic to be executed during shutdown.
     * @throws Exception if the shutdown task fails.
     */
    void run() throws Exception;

    /**
     * The number of seconds to wait for this task to complete before timing out.
     * If not overridden, the default component timeout from the {@link ShutdownCoordinator} will be used.
     * @return The timeout in seconds, or -1 to use the default.
     */
    default int getTimeoutSeconds() {
        return -1; // Use default timeout
    }
}
