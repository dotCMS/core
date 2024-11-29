package com.dotcms.rest.api.v1.job;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import org.glassfish.jersey.media.sse.EventOutput;

/**
 * Manages Server-Sent Events (SSE) connections for job monitoring. This class provides
 * functionality for tracking, limiting, and cleaning up SSE connections across multiple jobs.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Connection limits per job and system-wide</li>
 *   <li>Automatic connection timeout and cleanup</li>
 *   <li>Thread-safe connection management</li>
 *   <li>Proper resource cleanup on shutdown</li>
 * </ul>
 *
 * <p>Configuration properties:
 * <ul>
 *   <li>{@code MAX_SSE_CONNECTIONS_PER_JOB} - Maximum number of concurrent connections per job (default: 5)</li>
 *   <li>{@code MAX_SSE_TOTAL_CONNECTIONS} - Maximum total concurrent connections across all jobs (default: 50)</li>
 *   <li>{@code SSE_CONNECTION_TIMEOUT_MINUTES} - Connection timeout in minutes (default: 30)</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * SSEConnectionManager manager = new SSEConnectionManager();
 *
 * // Check if new connection can be accepted
 * if (manager.canAcceptNewConnection(jobId)) {
 *     // Add new connection
 *     manager.addConnection(jobId, eventOutput);
 * }
 *
 * // Close connections when job completes
 * manager.closeJobConnections(jobId);
 * }</pre>
 */
@ApplicationScoped
public class SSEConnectionManager {

    // Add status tracking
    private volatile boolean isShutdown = false;

    private static final Lazy<Integer> MAX_SSE_CONNECTIONS_PER_JOB =
            Lazy.of(() -> Config.getIntProperty("MAX_SSE_CONNECTIONS_PER_JOB", 5));

    private static final Lazy<Integer> MAX_SSE_TOTAL_CONNECTIONS =
            Lazy.of(() -> Config.getIntProperty("MAX_SSE_TOTAL_CONNECTIONS", 50));

    private static final Lazy<Integer> SSE_CONNECTION_TIMEOUT_MINUTES =
            Lazy.of(() -> Config.getIntProperty("SSE_CONNECTION_TIMEOUT_MINUTES", 30));

    private final ConcurrentMap<String, Set<SSEConnection>> jobConnections =
            new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutExecutor =
            Executors.newSingleThreadScheduledExecutor();

    /**
     * Shuts down the SSE connection manager and cleans up all resources. This method closes all
     * active connections and shuts down the timeout executor. After shutdown, no new connections
     * can be added.
     */
    @PreDestroy
    public void shutdown() {

        isShutdown = true;

        try {
            closeAllConnections();
        } finally {
            timeoutExecutor.shutdown();
            try {
                if (!timeoutExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    timeoutExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                timeoutExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Checks if a new SSE connection can be accepted for the given job. This method verifies both
     * per-job and system-wide connection limits.
     *
     * @param jobId The ID of the job for which to check connection availability
     * @return true if a new connection can be accepted, false otherwise
     */
    public boolean canAcceptNewConnection(final String jobId) {
        if (getTotalConnections() >= MAX_SSE_TOTAL_CONNECTIONS.get()) {
            return false;
        }

        Set<SSEConnection> connections = jobConnections.get(jobId);
        return connections == null || connections.size() < MAX_SSE_CONNECTIONS_PER_JOB.get();
    }

    /**
     * Adds a new SSE connection for a job. The connection will be automatically closed after the
     * configured timeout period.
     *
     * @param jobId       The ID of the job to monitor
     * @param eventOutput The EventOutput instance representing the SSE connection
     * @return The created SSEConnection instance
     * @throws IllegalStateException if the manager is shut down
     */
    public SSEConnection addConnection(final String jobId, final EventOutput eventOutput) {

        if (isShutdown) {
            throw new IllegalStateException("SSEConnectionManager is shut down");
        }

        SSEConnection connection = new SSEConnection(jobId, eventOutput);
        jobConnections.computeIfAbsent(jobId, k -> ConcurrentHashMap.newKeySet()).add(connection);

        // Schedule connection timeout
        timeoutExecutor.schedule(() -> {
            try {
                closeConnection(connection);
            } catch (Exception e) {
                Logger.error(this, "Error closing expired connection", e);
            }
        }, SSE_CONNECTION_TIMEOUT_MINUTES.get(), TimeUnit.MINUTES);

        return connection;
    }

    /**
     * Closes a specific SSE connection for a job. If this was the last connection for the job, the
     * job entry is removed from tracking.
     *
     * @param connection The connection to remove
     */
    public void closeConnection(final SSEConnection connection) {

        if (connection != null) {
            Set<SSEConnection> connections = jobConnections.get(connection.jobId);
            if (connections != null) {
                connections.remove(connection);
                connection.close();

                // If this was the last connection for the job, clean up the job entry
                if (connections.isEmpty()) {
                    jobConnections.remove(connection.jobId);
                }
            }
        }
    }

    /**
     * Gets the total number of active SSE connections across all jobs.
     *
     * @return The total number of active connections
     */
    private int getTotalConnections() {
        return jobConnections.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    /**
     * Closes all active SSE connections and clears connection tracking.
     */
    private void closeAllConnections() {
        jobConnections.values().forEach(connections ->
                connections.forEach(SSEConnection::close)
        );
        jobConnections.clear();
    }

    /**
     * Closes all SSE connections for a specific job.
     *
     * @param jobId The ID of the job whose connections should be closed
     */
    public void closeAllJobConnections(final String jobId) {
        Set<SSEConnection> connections = jobConnections.remove(jobId);
        if (connections != null) {
            connections.forEach(SSEConnection::close);
        }
    }

    /**
     * Gets the number of active connections for a specific job.
     *
     * @param jobId The ID of the job
     * @return The number of active connections for the job
     */
    public int getConnectionCount(final String jobId) {
        Set<SSEConnection> connections = jobConnections.get(jobId);
        return connections != null ? connections.size() : 0;
    }

    /**
     * Gets information about the current state of SSE connections.
     *
     * @return A map containing connection statistics:
     * - totalConnections: Total number of active connections
     * - activeJobs: Number of jobs with active connections
     */
    public Map<String, Object> getConnectionInfo() {
        return Map.of(
                "totalConnections", getTotalConnections(),
                "activeJobs", jobConnections.size()
        );
    }

    /**
     * Represents a single SSE connection for a job. Each connection tracks its creation time and
     * handles its own cleanup.
     */
    public static class SSEConnection {

        private final String jobId;
        private final EventOutput eventOutput;
        private final LocalDateTime createdAt;

        /**
         * Creates a new SSE connection.
         *
         * @param jobId       The ID of the job this connection is monitoring
         * @param eventOutput The EventOutput instance representing the SSE connection
         */
        public SSEConnection(String jobId, EventOutput eventOutput) {
            this.jobId = jobId;
            this.eventOutput = eventOutput;
            this.createdAt = LocalDateTime.now();
        }

        /**
         * Closes this SSE connection.
         */
        public void close() {
            try {
                eventOutput.close();
            } catch (IOException e) {
                Logger.error(SSEConnection.class, "Error closing SSE connection", e);
            }
        }

        /**
         * Checks if this connection has exceeded its timeout period.
         *
         * @return true if the connection has expired, false otherwise
         */
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(
                    createdAt.plusMinutes(SSE_CONNECTION_TIMEOUT_MINUTES.get()));
        }

        /**
         * Gets the ID of the job this connection is monitoring.
         *
         * @return The job ID
         */
        public String getJobId() {
            return jobId;
        }

        /**
         * Gets the EventOutput instance representing the SSE connection.
         *
         * @return The EventOutput instance
         */
        public EventOutput getEventOutput() {
            return eventOutput;
        }
    }

}
