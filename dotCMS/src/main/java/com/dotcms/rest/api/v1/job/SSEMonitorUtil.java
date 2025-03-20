package com.dotcms.rest.api.v1.job;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import com.dotcms.jobs.business.api.events.JobWatcher;
import com.dotcms.jobs.business.job.Job;
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
 * @see JobQueueHelper
 */
@ApplicationScoped
public class SSEMonitorUtil {

    private final JobQueueHelper helper;

    public SSEMonitorUtil() {
        // Default constructor required for CDI
        this.helper = null;
    }

    @Inject
    public SSEMonitorUtil(JobQueueHelper helper) {
        this.helper = helper;
    }

    /**
     * Sets up job monitoring via SSE
     *
     * @param jobId The job ID to monitor
     * @return EventOutput for streaming updates
     */
    @SuppressWarnings("java:S1854") // jobWatcher assignment is needed for cleanup in catch blocks
    public EventOutput monitorJob(final String jobId) {

        final var eventOutput = new EventOutput();
        final var resources = new MonitorResources(jobId, eventOutput, helper);

        try {

            Job job = helper.getJobForSSE(jobId);
            if (job == null) {
                sendErrorAndClose(SSEError.JOB_NOT_FOUND, resources);
                return eventOutput;
            }

            if (helper.isNotWatchable(job)) {
                sendErrorAndClose(SSEError.JOB_NOT_WATCHABLE, resources);
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

                        // If job is in a completed state, close the connection as no further
                        // updates will be available
                        if (helper.isTerminalState(watched.state())) {
                            resources.close();
                        }

                    } catch (IOException e) {
                        final var errorMessage = "Error writing SSE event";
                        Logger.error(this, errorMessage, e);

                        // Make sure to close the connection
                        resources.close();

                        // Re-throw the IOException to be caught by the outer catch block in the
                        // RealTimeJobMonitor that will clean up the job watcher
                        throw new DotRuntimeException(errorMessage, e);
                    }
                }
            };

            // Start watching the job
            final var jobWatcher = helper.watchJob(job.id(), jobWatcherConsumer);
            resources.jobWatcher(jobWatcher);

            return eventOutput;
        } catch (Exception e) {
            final var errorMessage = "Error setting up job monitor";
            Logger.error(this, errorMessage, e);

            // Make sure to close the connection and remove the job watcher
            resources.close();

            throw new DotRuntimeException(errorMessage, e);
        }
    }

    /**
     * Send an error event and close the connection
     *
     * @param error     The error to send
     * @param resources The current monitoring resources
     * @throws IOException If there is an error writing the event
     */
    private void sendErrorAndClose(final SSEError error, MonitorResources resources)
            throws IOException {
        OutboundEvent event = new OutboundEvent.Builder()
                .mediaType(MediaType.TEXT_HTML_TYPE)
                .name(error.getName())
                .data(String.class, String.valueOf(error.getCode()))
                .build();
        resources.eventOutput().write(event);
        resources.close();
    }

    /**
     * Enumeration representing various SSE (Server-Sent Events) error states with associated error
     * names and HTTP status codes. It is used to identify specific error conditions related to job
     * monitoring.
     */
    private enum SSEError {

        JOB_NOT_FOUND("job-not-found", NOT_FOUND.getStatusCode()),
        JOB_NOT_WATCHABLE("job-not-watchable", BAD_REQUEST.getStatusCode());

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
     * A resource management class that handles cleanup of SSE monitoring resources.
     */
    private static class MonitorResources {

        private final EventOutput eventOutput;
        private JobWatcher jobWatcher;
        private final String jobId;
        private final JobQueueHelper helper;

        /**
         * Creates a new MonitorResources instance to manage SSE monitoring resources.
         *
         * @param jobId The ID of the job being monitored
         * @param eventOutput The SSE connection for job updates
         * @param helper Helper for job queue operations
         */
        MonitorResources(String jobId, EventOutput eventOutput, JobQueueHelper helper) {
            this.jobId = jobId;
            this.eventOutput = eventOutput;
            this.helper = helper;
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
         * Gets the SSE connection for this monitoring session.
         *
         * @return The SSE connection
         */
        EventOutput eventOutput() {
            return eventOutput;
        }

        /**
         * Closes and cleans up all monitoring resources. This includes closing the SSE connection
         * and removing the job watcher if one exists.
         */
        void close() {
            if (eventOutput != null) {
                try {
                    eventOutput.close();
                } catch (IOException e) {
                    Logger.error(MonitorResources.class, "Error closing event output", e);
                }
            }
            if (jobWatcher != null) {
                helper.removeWatcher(jobId, jobWatcher);
            }
        }
    }

}
