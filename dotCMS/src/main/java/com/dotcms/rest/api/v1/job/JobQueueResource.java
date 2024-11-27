package com.dotcms.rest.api.v1.job;

import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.VisibleForTesting;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

@Path("/v1/jobs")
public class JobQueueResource {

    private final WebResource webResource;
    private final JobQueueHelper helper;
    private final SSEConnectionManager sseConnectionManager;

    @Inject
    public JobQueueResource(final JobQueueHelper helper,
            final SSEConnectionManager sseConnectionManager) {
        this(new WebResource(), helper, sseConnectionManager);
    }

    @VisibleForTesting
    public JobQueueResource(WebResource webResource, JobQueueHelper helper,
            SSEConnectionManager sseConnectionManager) {
        this.webResource = webResource;
        this.helper = helper;
        this.sseConnectionManager = sseConnectionManager;
    }

    @POST
    @Path("/{queueName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(
            @Context HttpServletRequest request,
            @PathParam("queueName") String queueName,
            @BeanParam JobParams form) throws JsonProcessingException, DotDataException {

        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();

        try {
            final String jobId = helper.createJob(
                    queueName, form, initDataObject.getUser(), request);
            return Response.ok(new ResponseEntityView<>(jobId)).build();
        } catch (JobValidationException e) {
            return ExceptionMapperUtil.createResponse(null, e.getMessage());
        }
    }

    @POST
    @Path("/{queueName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(
            @Context HttpServletRequest request,
            @PathParam("queueName") String queueName,
            Map<String, Object> parameters) throws DotDataException {

        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();

        try {
            final String jobId = helper.createJob(
                    queueName, parameters, initDataObject.getUser(), request);
            return Response.ok(new ResponseEntityView<>(jobId)).build();
        } catch (JobValidationException e) {
            return ExceptionMapperUtil.createResponse(null, e.getMessage());
        }
    }

    @GET
    @Path("/queues")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<Set<String>> getQueues(@Context HttpServletRequest request) {
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        return new ResponseEntityView<>(helper.getQueueNames());
    }

    @GET
    @Path("/{jobId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<Job> getJobStatus(@Context HttpServletRequest request,
            @PathParam("jobId") String jobId)
            throws DotDataException {

        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();

        Job job = helper.getJob(jobId);
        return new ResponseEntityView<>(job);
    }

    @POST
    @Path("/{jobId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public ResponseEntityView<String> cancelJob(@Context HttpServletRequest request,
            @PathParam("jobId") String jobId) throws DotDataException {
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        helper.cancelJob(jobId);
        return new ResponseEntityView<>("Cancellation request successfully sent to job " + jobId);
    }

    @GET
    @Path("/{queueName}/active")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> activeJobs(@Context HttpServletRequest request,
            @PathParam("queueName") String queueName,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getActiveJobs(queueName, page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/{jobId}/monitor")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput monitorJob(@Context HttpServletRequest request,
            @PathParam("jobId") String jobId) {

        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();

        final EventOutput eventOutput = new EventOutput();

        try {
            Job job = helper.getJobForSSE(jobId);

            if (job == null) {
                helper.sendErrorAndClose("job-not-found", "404", eventOutput);
                return eventOutput;
            }

            if (helper.isNotWatchable(job)) {
                helper.sendErrorAndClose(String.format("job-not-watchable [%s]",
                        job.state()), "400", eventOutput);
                return eventOutput;
            }

            if (!sseConnectionManager.canAcceptNewConnection(jobId)) {
                helper.sendErrorAndClose("too-many-connections", "429", eventOutput);
                return eventOutput;
            }

            // Callback for watching job updates and sending them to the client
            Consumer<Job> jobWatcher = watched -> {
                if (!eventOutput.isClosed()) {
                    try {
                        OutboundEvent event = new OutboundEvent.Builder()
                                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                .name("job-update")
                                .data(Map.class, helper.getJobStatusInfo(watched))
                                .build();
                        eventOutput.write(event);

                        // If job is complete/failed/cancelled, close the connection
                        if (helper.isTerminalState(watched.state())) {
                            sseConnectionManager.closeJobConnections(jobId);
                        }

                    } catch (IOException e) {
                        Logger.error(this, "Error writing SSE event", e);
                        sseConnectionManager.closeJobConnections(jobId);
                    }
                }
            };

            // Register the connection and watcher
            sseConnectionManager.addConnection(jobId, eventOutput);

            // Start watching the job
            helper.watchJob(job.id(), jobWatcher);

        } catch (DotDataException e) {
            Logger.error(this, "Error setting up job monitor", e);
            helper.closeSSEConnection(eventOutput);
        }

        return eventOutput;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> listJobs(@Context HttpServletRequest request,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/active")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> activeJobs(@Context HttpServletRequest request,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getActiveJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/completed")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> completedJobs(@Context HttpServletRequest request,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getCompletedJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/canceled")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> canceledJobs(@Context HttpServletRequest request,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getCanceledJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/failed")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> failedJobs(@Context HttpServletRequest request,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getFailedJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

}