package com.dotcms.rest.api.v1.job;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.TOO_MANY_REQUESTS;

import com.dotcms.jobs.business.api.events.JobWatcher;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.rest.api.v1.job.SSEConnectionManager.SSEConnection;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.OutboundEvent.Builder;

/**
 * Utility class for managing Server-Sent Events (SSE) job monitoring. This class handles the setup,
 * maintenance, and cleanup of SSE connections for monitoring job progress and status updates.
 *
 * <p>Key responsibilities include:
 * <ul>
 *   <li>Setting up SSE connections for job monitoring</li>
 *   <li>Managing job watchers and event streams</li>
 *   <li>Handling error conditions and connection cleanup</li>
 *   <li>Coordinating between job updates and SSE event publishing</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * @Inject
 * private SSEMonitorUtil sseMonitorUtil;
 *
 * // Set up job monitoring
 * EventOutput eventOutput = sseMonitorUtil.monitorJob(jobId);
 * }</pre>
 *
 * <p>This class is thread-safe and can handle multiple concurrent monitoring sessions.
 * It automatically manages resource cleanup through the {@link SSEConnectionManager} and
 * ensures proper handling of connection lifecycles.
 *
 * @see SSEConnectionManager
 * @see JobQueueHelper
 */
@ApplicationScoped
public class SSEMonitorUtil {

    private final JobQueueHelper helper;
    private final SSEConnectionManager sseConnectionManager;

    public SSEMonitorUtil() {
        // Default constructor required for CDI
        this.helper = null;
        this.sseConnectionManager = null;
    }

    @Inject
    public SSEMonitorUtil(JobQueueHelper helper, SSEConnectionManager sseConnectionManager) {
        this.helper = helper;
        this.sseConnectionManager = sseConnectionManager;
    }

    /**
     * Sets up job monitoring via SSE
     *
     * @param jobId The job ID to monitor
     * @return EventOutput for streaming updates
     */
    @SuppressWarnings("java:S1854") // jobWatcher assignment is needed for cleanup in catch blocks
    public EventOutput monitorJob(final String jobId) {

        JobWatcher jobWatcher = null;
        final EventOutput eventOutput = new EventOutput();
        final var connection = sseConnectionManager.addConnection(jobId, eventOutput);

        try (final var resources =
                new MonitorResources(jobId, connection, helper, sseConnectionManager)) {

            Job job = helper.getJobForSSE(jobId);
            if (job == null) {
                sendError(SSEError.JOB_NOT_FOUND, connection);
                return eventOutput;
            }

            if (helper.isNotWatchable(job)) {
                sendError(SSEError.JOB_NOT_WATCHABLE, connection);
                return eventOutput;
            }

            if (!sseConnectionManager.canAcceptNewConnection(jobId)) {
                sendError(SSEError.TOO_MANY_CONNECTIONS, connection);
                return eventOutput;
            }

            // Callback for watching job updates and sending them to the client
            Consumer<Job> jobWatcherConsumer = watched -> {
                if (!eventOutput.isClosed()) {
                    try {
                        OutboundEvent event = new Builder()
                                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                .name("job-update")
                                .data(Map.class, helper.getJobStatusInfo(watched))
                                .build();
                        eventOutput.write(event);

                        // If job is in a completed state, close all connections as no further
                        // updates will be available
                        if (helper.isTerminalState(watched.state())) {
                            sseConnectionManager.closeAllJobConnections(jobId);
                        }

                    } catch (IOException e) {
                        final var errorMessage = "Error writing SSE event";
                        Logger.error(this, errorMessage, e);
                        // Re-throw the IOException to be caught by the outer catch block in the
                        // RealTimeJobMonitor that will clean up the job watcher
                        throw new DotRuntimeException(errorMessage, e);
                    }
                }
            };

            // Start watching the job
            jobWatcher = helper.watchJob(job.id(), jobWatcherConsumer);
            resources.jobWatcher(jobWatcher);

            return eventOutput;
        } catch (Exception e) {
            final var errorMessage = "Error setting up job monitor";
            Logger.error(this, errorMessage, e);
            throw new DotRuntimeException(errorMessage, e);
        }
    }

    /**
     * Send an error event and close the connection
     *
     * @param error     The error to send
     * @param connection The SSE connection to close
     * @throws IOException If there is an error writing the event
     */
    private void sendError(final SSEError error, final SSEConnection connection)
            throws IOException {
        OutboundEvent event = new OutboundEvent.Builder()
                .mediaType(MediaType.TEXT_HTML_TYPE)
                .name(error.getName())
                .data(String.class, String.valueOf(error.getCode()))
                .build();
        connection.getEventOutput().write(event);
    }

    /**
     * Enumeration representing various SSE (Server-Sent Events) error states with associated error
     * names and HTTP status codes. It is used to identify specific error conditions related to job
     * monitoring.
     */
    private enum SSEError {

        JOB_NOT_FOUND("job-not-found", NOT_FOUND.getStatusCode()),
        JOB_NOT_WATCHABLE("job-not-watchable", BAD_REQUEST.getStatusCode()),
        TOO_MANY_CONNECTIONS("too-many-connections", TOO_MANY_REQUESTS.getStatusCode());

        private final String name;
        private final int code;

        SSEError(String name, int code) {
            this.name = name;
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * A resource management class that handles cleanup of SSE monitoring resources. This class
     * implements AutoCloseable to ensure proper cleanup of both SSE connections and job watchers
     * through try-with-resources blocks.
     *
     * <p>This class manages:
     * <ul>
     *   <li>SSE connection lifecycle</li>
     *   <li>Job watcher registration and cleanup</li>
     *   <li>Automatic resource cleanup when monitoring ends or errors occur</li>
     * </ul>
     */
    private static class MonitorResources implements AutoCloseable {

        private final SSEConnection connection;
        private JobWatcher jobWatcher;
        private final String jobId;
        private final JobQueueHelper helper;
        private final SSEConnectionManager sseConnectionManager;

        /**
         * Creates a new MonitorResources instance to manage SSE monitoring resources.
         *
         * @param jobId The ID of the job being monitored
         * @param connection The SSE connection to manage
         * @param helper Helper for job queue operations
         * @param sseConnectionManager Manager for SSE connections
         */
        MonitorResources(String jobId, SSEConnection connection, JobQueueHelper helper,
                SSEConnectionManager sseConnectionManager) {
            this.jobId = jobId;
            this.connection = connection;
            this.helper = helper;
            this.sseConnectionManager = sseConnectionManager;
        }

        /**
         * Sets the job watcher for this monitoring session.
         *
         * @param watcher The job watcher to associate with this monitoring session
         */
        void jobWatcher(JobWatcher watcher) {
            this.jobWatcher = watcher;
        }

        /**
         * Closes and cleans up all monitoring resources. This includes closing the SSE connection
         * and removing the job watcher if one exists.
         */
        @Override
        public void close() {
            if (connection != null) {
                sseConnectionManager.closeConnection(connection);
            }
            if (jobWatcher != null) {
                helper.removeWatcher(jobId, jobWatcher);
            }
        }
    }

}
