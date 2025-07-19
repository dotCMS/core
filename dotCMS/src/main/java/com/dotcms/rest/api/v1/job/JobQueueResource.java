package com.dotcms.rest.api.v1.job;

import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.rest.ResponseEntityJobPaginatedResultView;
import com.dotcms.rest.ResponseEntitySetStringView;
import com.dotcms.rest.ResponseEntityJobStatusView;
import com.dotcms.rest.ResponseEntityJobView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.VisibleForTesting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@SwaggerCompliant(value = "Publishing and content distribution APIs", batch = 5)
@Path("/v1/jobs")
@Tag(name = "Job Queue")
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
    @Operation(
            operationId = "createJobWithFormData",
            summary = "Creates a new job with form data",
            description = "Creates and queues a new background job with multipart form data parameters. " +
                    "Returns the job ID and initial status information.",
            tags = {"Job Queue"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobStatusView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid job parameters or queue name"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response createJob(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Name of the job queue to submit to") @PathParam("queueName") String queueName,
            @Parameter(description = "Job parameters as multipart form data") @BeanParam JobParams form) throws JsonProcessingException, DotDataException {

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
    @Operation(
            operationId = "createJobWithJson",
            summary = "Creates a new job with JSON parameters",
            description = "Creates and queues a new background job with JSON parameters. " +
                    "Returns the job ID and initial status information.",
            tags = {"Job Queue"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobStatusView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid job parameters or queue name"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response createJob(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Name of the job queue to submit to") @PathParam("queueName") String queueName,
            @RequestBody(description = "Job parameters as JSON key-value pairs",
                    required = true,
                    content = @Content(schema = @Schema(type = "object", description = "JSON object containing job parameters as key-value pairs", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)))
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
    @Operation(
            operationId = "getQueues",
            summary = "Retrieves available job queues",
            description = "Returns a list of all available job queue names that can be used for submitting jobs.",
            tags = {"Job Queue"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Queues retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntitySetStringView.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<Set<String>> getQueues(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        return new ResponseEntitySetStringView(helper.getQueueNames());
    }

    @GET
    @Path("/{jobId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getJobStatus",
            summary = "Retrieves job status information",
            description = "Returns detailed status information for a specific job including progress, state, and results.",
            tags = {"Job Queue"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job status retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid job ID"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "404", description = "Job not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<Job> getJobStatus(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the job") @PathParam("jobId") String jobId) throws DotDataException {

        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Job job = helper.getJob(jobId);
        return new ResponseEntityJobView(job);
    }

    @Operation(
        summary = "Cancel job",
        description = "Sends a cancellation request to the specified job. The job may not immediately stop if it's currently running."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Cancellation request sent successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid job ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Job not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/{jobId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityStringView cancelJob(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the job to cancel", required = true) @PathParam("jobId") String jobId) throws DotDataException {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        helper.cancelJob(jobId);
        return new ResponseEntityStringView("Cancellation request successfully sent to job " + jobId);
    }

    @Operation(
        summary = "Get active jobs by queue",
        description = "Returns paginated list of currently active/running jobs in the specified queue."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Active jobs retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid queue name or pagination parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{queueName}/active")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> activeJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Name of the job queue to filter by", required = true) @PathParam("queueName") String queueName,
            @Parameter(description = "Page number for pagination (default: 1)") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Number of jobs per page (default: 20)") @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getActiveJobs(queueName, page, pageSize);
        return new ResponseEntityJobPaginatedResultView(result);
    }

    @Operation(
        summary = "List all jobs",
        description = "Returns paginated list of all jobs across all queues regardless of status."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Jobs retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid pagination parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> listJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Page number for pagination (default: 1)") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Number of jobs per page (default: 20)") @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getJobs(page, pageSize);
        return new ResponseEntityJobPaginatedResultView(result);
    }

    @Operation(
        summary = "Get all active jobs",
        description = "Returns paginated list of currently active/running jobs across all queues."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Active jobs retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid pagination parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/active")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> activeJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Page number for pagination (default: 1)") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Number of jobs per page (default: 20)") @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getActiveJobs(page, pageSize);
        return new ResponseEntityJobPaginatedResultView(result);
    }

    @Operation(
        summary = "Get completed jobs",
        description = "Returns paginated list of jobs that have completed successfully."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Completed jobs retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid pagination parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/completed")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> completedJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Page number for pagination (default: 1)") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Number of jobs per page (default: 20)") @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getCompletedJobs(page, pageSize);
        return new ResponseEntityJobPaginatedResultView(result);
    }

    @Operation(
        summary = "Get successful jobs",
        description = "Returns paginated list of jobs that have completed successfully without errors."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Successful jobs retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid pagination parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/successful")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> successfulJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Page number for pagination (default: 1)") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Number of jobs per page (default: 20)") @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getSuccessfulJobs(page, pageSize);
        return new ResponseEntityJobPaginatedResultView(result);
    }

    @Operation(
        summary = "Get canceled jobs",
        description = "Returns paginated list of jobs that have been canceled before completion."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Canceled jobs retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid pagination parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/canceled")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> canceledJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Page number for pagination (default: 1)") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Number of jobs per page (default: 20)") @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getCanceledJobs(page, pageSize);
        return new ResponseEntityJobPaginatedResultView(result);
    }

    @Operation(
        summary = "Get failed jobs",
        description = "Returns paginated list of jobs that have failed due to errors during execution."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Failed jobs retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid pagination parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/failed")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> failedJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Page number for pagination (default: 1)") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Number of jobs per page (default: 20)") @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getFailedJobs(page, pageSize);
        return new ResponseEntityJobPaginatedResultView(result);
    }

    @Operation(
        summary = "Get abandoned jobs",
        description = "Returns paginated list of jobs that have been abandoned (no longer tracked or monitored)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Abandoned jobs retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid pagination parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/abandoned")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> abandonedJobs(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Page number for pagination (default: 1)") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Number of jobs per page (default: 20)") @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getAbandonedJobs(page, pageSize);
        return new ResponseEntityJobPaginatedResultView(result);
    }

    @Operation(
        summary = "Monitor job progress",
        description = "Establishes a Server-Sent Events (SSE) connection to monitor real-time progress of a specific job. Returns continuous updates until job completion."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "SSE connection established for job monitoring",
                    content = @Content(mediaType = "text/event-stream")),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid job ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Job not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{jobId}/monitor")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @SuppressWarnings("java:S1854") // jobWatcher assignment is needed for cleanup in catch blocks
    public EventOutput monitorJob(
            @Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the job to monitor", required = true) @PathParam("jobId") String jobId) {

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