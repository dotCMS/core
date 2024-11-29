package com.dotcms.rest.api.v1.job;

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

        try {

            Job job = helper.getJobForSSE(jobId);
            if (job == null) {
                sendErrorAndClose("job-not-found", "404", connection);
                return eventOutput;
            }

            if (helper.isNotWatchable(job)) {
                sendErrorAndClose(String.format("job-not-watchable [%s]",
                        job.state()), "400", connection);
                return eventOutput;
            }

            if (!sseConnectionManager.canAcceptNewConnection(jobId)) {
                sendErrorAndClose("too-many-connections", "429", connection);
                return eventOutput;
            }

            // Callback for watching job updates and sending them to the client
            Consumer<Job> jobWatcherConsumer = watched -> {
                if (!eventOutput.isClosed()) {
                    OutboundEvent event = new Builder()
                            .mediaType(MediaType.APPLICATION_JSON_TYPE)
                            .name("job-update")
                            .data(Map.class, helper.getJobStatusInfo(watched))
                            .build();

                    try {
                        eventOutput.write(event);
                    } catch (IOException e) {
                        final var errorMessage = "Error writing SSE event";
                        Logger.error(this, errorMessage, e);
                        // Re-throw the IOException to be caught by the outer catch block
                        throw new DotRuntimeException(errorMessage, e);
                    }

                    // If job is in a completed state, close all connections as no further
                    // updates will be available
                    if (helper.isTerminalState(watched.state())) {
                        sseConnectionManager.closeAllJobConnections(jobId);
                    }
                }
            };

            // Start watching the job
            jobWatcher = helper.watchJob(job.id(), jobWatcherConsumer);

            return eventOutput;
        } catch (IOException e) {
            final var errorMessage = "Error writing SSE event";
            Logger.error(this, errorMessage, e);
            cleanupOnError(jobId, connection, jobWatcher);
            throw new DotRuntimeException(errorMessage, e);
        } catch (Exception e) {
            final var errorMessage = "Error setting up job monitor";
            Logger.error(this, errorMessage, e);
            cleanupOnError(jobId, connection, jobWatcher);
            throw new DotRuntimeException(errorMessage, e);
        }
    }

    /**
     * Send an error event and close the connection
     *
     * @param errorName  The name of the error event
     * @param errorCode  The error code
     * @param connection The SSE connection to close
     * @throws IOException If there is an error writing the event
     */
    private void sendErrorAndClose(final String errorName, final String errorCode,
            final SSEConnection connection) throws IOException {
        OutboundEvent event = new OutboundEvent.Builder()
                .mediaType(MediaType.TEXT_HTML_TYPE)
                .name(errorName)
                .data(String.class, errorCode)
                .build();
        connection.getEventOutput().write(event);
        sseConnectionManager.closeConnection(connection);
    }

    /**
     * Clean up resources after an error
     */
    private void cleanupOnError(final String jobId, final SSEConnection connection,
            final JobWatcher jobWatcher) {
        if (connection != null) {
            sseConnectionManager.closeConnection(connection);
        }
        if (jobWatcher != null) {
            helper.removeWatcher(jobId, jobWatcher);
        }
    }

}
