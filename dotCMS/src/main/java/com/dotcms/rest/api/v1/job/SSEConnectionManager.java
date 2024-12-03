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

    // Add per-job connection limit, default to 5 and -1 to disable
    private static final Lazy<Integer> MAX_SSE_CONNECTIONS_PER_JOB =
            Lazy.of(() -> Config.getIntProperty("MAX_SSE_CONNECTIONS_PER_JOB", 5));

    // Add total connection limit, default to 50 and -1 to disable
    private static final Lazy<Integer> MAX_SSE_TOTAL_CONNECTIONS =
            Lazy.of(() -> Config.getIntProperty("MAX_SSE_TOTAL_CONNECTIONS", 50));

    private final ConcurrentMap<String, Set<SSEConnection>> jobConnections =
            new ConcurrentHashMap<>();

    /**
     * Shuts down the SSE connection manager and cleans up all resources. This method closes all
     * active connections. After shutdown, no new connections can be added.
     */
    @PreDestroy
    public void shutdown() {
        isShutdown = true;
        closeAllConnections();
    }

    /**
     * Checks if a new SSE connection can be accepted for the given job. This method verifies both
     * per-job and system-wide connection limits if enabled (not -1).
     *
     * @param jobId The ID of the job for which to check connection availability
     * @return true if a new connection can be accepted, false otherwise
     */
    public boolean canAcceptNewConnection(final String jobId) {

        final var maxSseTotalConnections = MAX_SSE_TOTAL_CONNECTIONS.get();
        final var maxSseConnectionsPerJob = MAX_SSE_CONNECTIONS_PER_JOB.get();

        // Check total connections limit if enabled (not -1)
        if (maxSseTotalConnections != -1 && getTotalConnections() >= maxSseTotalConnections) {
            return false;
        }

        // If per-job limit is disabled (-1), allow connection
        if (maxSseConnectionsPerJob == -1) {
            return true;
        }

        // Check per-job limit
        Set<SSEConnection> connections = jobConnections.get(jobId);
        return connections == null || connections.size() < maxSseConnectionsPerJob;
    }

    /**
     * Adds a new SSE connection for a job. The connection will be automatically closed after the
     * configured timeout period if timeout is enabled (not -1).
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
