package com.dotcms.rest.api.v1.job;

import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.rest.ResponseEntityJobStatusView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.VisibleForTesting;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.glassfish.jersey.media.sse.SseFeature;

@Path("/v1/jobs")
public class JobQueueResource {

    private final WebResource webResource;
    private final JobQueueHelper helper;
    private final SSEMonitorUtil sseMonitorUtil;

    @Inject
    public JobQueueResource(final JobQueueHelper helper, final SSEMonitorUtil sseMonitorUtil) {
        this(new WebResource(), helper, sseMonitorUtil);
    }

    @VisibleForTesting
    public JobQueueResource(WebResource webResource, JobQueueHelper helper,
            final SSEMonitorUtil sseMonitorUtil) {
        this.webResource = webResource;
        this.helper = helper;
        this.sseMonitorUtil = sseMonitorUtil;
    }

    @POST
    @Path("/{queueName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("queueName") String queueName,
            @BeanParam JobParams form) throws JsonProcessingException, DotDataException {

        final var initDataObject = new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        try {
            final String jobId = helper.createJob(
                    queueName, form, initDataObject.getUser(), request);
            final var jobStatusResponse = helper.buildJobStatusResponse(jobId, request);
            return Response.ok(new ResponseEntityJobStatusView(jobStatusResponse)).build();
        } catch (JobValidationException e) {
            return ExceptionMapperUtil.createResponse(null, e.getMessage());
        }
    }

    @POST
    @Path("/{queueName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("queueName") String queueName,
            Map<String, Object> parameters) throws DotDataException {

        final var initDataObject = new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        try {
            final String jobId = helper.createJob(
                    queueName, parameters, initDataObject.getUser(), request);
            final var jobStatusResponse = helper.buildJobStatusResponse(jobId, request);
            return Response.ok(new ResponseEntityJobStatusView(jobStatusResponse)).build();
        } catch (JobValidationException e) {
            return ExceptionMapperUtil.createResponse(null, e.getMessage());
        }
    }

    @GET
    @Path("/queues")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<Set<String>> getQueues(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        return new ResponseEntityView<>(helper.getQueueNames());
    }

    @GET
    @Path("/{jobId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<Job> getJobStatus(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("jobId") String jobId) throws DotDataException {

        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Job job = helper.getJob(jobId);
        return new ResponseEntityView<>(job);
    }

    @POST
    @Path("/{jobId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public ResponseEntityView<String> cancelJob(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("jobId") String jobId) throws DotDataException {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        helper.cancelJob(jobId);
        return new ResponseEntityView<>("Cancellation request successfully sent to job " + jobId);
    }

    @GET
    @Path("/{queueName}/active")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> activeJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("queueName") String queueName,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getActiveJobs(queueName, page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> listJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/active")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> activeJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getActiveJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/completed")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> completedJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getCompletedJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/successful")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> successfulJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getSuccessfulJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/canceled")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> canceledJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getCanceledJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/failed")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> failedJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getFailedJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/abandoned")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> abandonedJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getAbandonedJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/{jobId}/monitor")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @SuppressWarnings("java:S1854") // jobWatcher assignment is needed for cleanup in catch blocks
    public EventOutput monitorJob(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("jobId") String jobId) {

        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        // Set up job monitoring
        return sseMonitorUtil.monitorJob(jobId);
    }

}