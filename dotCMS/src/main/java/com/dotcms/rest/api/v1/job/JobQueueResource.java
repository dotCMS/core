package com.dotcms.rest.api.v1.job;

import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.rest.*;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.VisibleForTesting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

@Path("/v1/jobs")
@Tag(name = "Job Queue", description = "Endpoints for managing background jobs and job queues")
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

    /**
     * Creates and enqueues a job whose parameters are supplied as a multipart
     * form. Typical use-cases include uploading a file together with a JSON
     * payload of options.
     */
    @POST
    @Path("/{queueName}/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "createJobWithFormData",
            summary = "Creates a new job with form data",
            description = "Creates and queues a new background job with multipart form data parameters. " +
                    "Returns the job ID and initial status information.",
            tags = {"Job Queue"},
            requestBody = @RequestBody(
                    description = JobQueueDocs.FORM_FIELD_DOC,
                    required = false,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA,
                            schema = @Schema(implementation = JobParamsSchema.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job created successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseEntityJobStatusView.class),
                                    examples = @ExampleObject(value = "{\n" +
                                            "  \"entity\": {\n" +
                                            "    \"jobId\": \"e6d9bae8-657b-4e2f-8524-c0222db66355\",\n" +
                                            "    \"statusUrl\": \"http://localhost:8080/api/v1/_import/e6d9bae8-657b-4e2f-8524-c0222db66355\"\n" +
                                            "  },\n" +
                                            "  \"errors\": [],\n" +
                                            "  \"i18nMessagesMap\": {},\n" +
                                            "  \"messages\": [],\n" +
                                            "  \"pagination\": null,\n" +
                                            "  \"permissions\": []\n" +
                                            "}")
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request: Invalid parameters, malformed multipart payload, or file issues."),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User lacks permission to enqueue jobs in the specified queue."),
                    @ApiResponse(responseCode = "404", description = "Not Found: Queue with the specified name does not exist."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while creating the job.")
            }
    )
    public Response createJob(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("queueName")
            @Parameter(
                    name        = "queueName",
                    in          = ParameterIn.PATH,
                    required    = true,
                    description = "Name of the job queue to submit to",
                    schema      = @Schema(type = "string", example = "image-processing")
            )
            String queueName,
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

    /**
     * REST resource for queueing arbitrary jobs.
     *
     * <p>This endpoint accepts a JSON payload with job-specific parameters and
     * enqueues the job on the specified queue, returning a handle that can later
     * be polled for status updates.</p>
     */
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
            requestBody = @RequestBody(
                    required    = true,
                    description = "Opaque key/value map with job-specific parameters.",
                    content     = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            examples  = @ExampleObject(
                                    name  = "ThumbnailJob",
                                    value = "{\n"
                                            + "  \"contentType\": \"CustomContentType\",\n"
                                            + "  \"workflowActionId\": \"Workflow-UUID\",\n"
                                            + "  \"language\": \"en-us\",\n"
                                            + "  \"stopOnError\": true\n"
                                            + "}"
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobStatusView.class),
                                    examples = @ExampleObject(value = "{\n" +
                                            "  \"entity\": {\n" +
                                            "    \"jobId\": \"e6d9bae8-657b-4e2f-8524-c0222db66355\",\n" +
                                            "    \"statusUrl\": \"http://localhost:8080/api/v1/_import/e6d9bae8-657b-4e2f-8524-c0222db66355\"\n" +
                                            "  },\n" +
                                            "  \"errors\": [],\n" +
                                            "  \"i18nMessagesMap\": {},\n" +
                                            "  \"messages\": [],\n" +
                                            "  \"pagination\": null,\n" +
                                            "  \"permissions\": []\n" +
                                            "}")
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request: Invalid parameters or malformed JSON."),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User lacks permission to enqueue jobs in the specified queue."),
                    @ApiResponse(responseCode = "404", description = "Not Found: Queue with the specified name does not exist."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while creating the job.")
            }
    )
    public Response createJob(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("queueName")
            @Parameter(
                    name        = "queueName",
                    in          = ParameterIn.PATH,
                    required    = true,
                    description = "Name of the job queue to submit to",
                    schema      = @Schema(type = "string", example = "image-processing")
            )
            String queueName,
            @RequestBody(description = "Job parameters as JSON key-value pairs",
                    required = true,
                    content = @Content(schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)))
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

    /**
     * Lists all available job queues.
     */
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
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<Set<String>> getQueues(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        return new ResponseEntityView<>(helper.getQueueNames());
    }

    /**
     * Retrieves the status of a job based on the provided job ID.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param jobId The ID of the job whose status is to be retrieved.
     * @return A ResponseEntityView containing the job status.
     * @throws DotDataException If there is an issue with DotData during the retrieval process.
     */
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
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid job ID"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "404", description = "Job not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<Job> getJobStatus(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("jobId")
            @Parameter(
                    name        = "jobId",
                    in          = ParameterIn.PATH,
                    required    = true,
                    description = "Unique identifier (UUID) of the job.",
                    schema      = @Schema(type = "string", format = "uuid", example = "e6d9bae8-657b-4e2f-8524-c0222db66355")
            )
            String jobId) throws DotDataException {

        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Job job = helper.getJob(jobId);
        return new ResponseEntityView<>(job);
    }

    /**
     * Cancels a job based on the provided job ID.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param jobId The ID of the job to be canceled.
     * @return A ResponseEntityView containing a message indicating the cancellation status.
     * @throws DotDataException If there is an issue with DotData during the cancellation process.
     */
    @POST
    @Path("/{jobId}/cancel")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "cancelJobQueueJob",
            summary     = "Cancel a job",
            description = "Sends an asynchronous cancellation request for the specified job. "
                    + "The job may still complete if it has already finished or cannot be interrupted.",
            tags        = {"Job Queue"},
            responses   = {
                    @ApiResponse(
                            responseCode = "200",
                            description  = "Cancellation request accepted.",
                            content      = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema    = @Schema(implementation = ResponseEntityStringView.class),
                                    examples  = @ExampleObject(
                                            value = "{\n"
                                                    + "  \"entity\": "
                                                    + "\"Cancellation request successfully sent to job "
                                                    + "e6d9bae8-657b-4e2f-8524-c0222db66355\"\n"
                                                    + "}"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have permission to cancel this job."),
                    @ApiResponse(responseCode = "404", description = "Not Found: Job with the specified ID does not exist or is already completed/cancelled."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while attempting to cancel the job.")
            }
    )
    public ResponseEntityView<String> cancelJob(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("jobId")
            @Parameter(
                    name        = "jobId",
                    in          = ParameterIn.PATH,
                    required    = true,
                    description = "Unique identifier (UUID) of the job to cancel.",
                    schema      = @Schema(type = "string", format = "uuid", example = "e6d9bae8-657b-4e2f-8524-c0222db66355")
            )
            String jobId) throws DotDataException {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        helper.cancelJob(jobId);
        return new ResponseEntityView<>("Cancellation request successfully sent to job " + jobId);
    }

    /**
     * Retrieves the status of active jobs on a given queue.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param queueName The name of the queue to search for active jobs.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of active jobs.
     */
    @GET
    @Path("/{queueName}/active")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getActiveJobsByQueue",
            summary     = "Retrieves active jobs in a queue",
            description = "Fetches a paginated list of active jobs. Results can be paginated using query parameters."
                    + "for the specified queue.",
            tags        = {"Job Queue"},
            responses   = {
                    @ApiResponse(
                            responseCode = "200",
                            description  = "Successfully retrieved active jobs for the queue.",
                            content      = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema    = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have permission to view jobs in this queue."),
                    @ApiResponse(responseCode = "404", description = "Not Found: Queue with the specified name does not exist."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while retrieving jobs.")
            }
    )
    public ResponseEntityView<JobPaginatedResult> activeJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("queueName")
            @Parameter(
                    name        = "queueName",
                    in          = ParameterIn.PATH,
                    required    = true,
                    description = "Logical name of the queue.",
                    schema      = @Schema(type = "string", example = "image-processing")
            )
            String queueName,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(
                    description = "Number of records per page."
            )
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

    /**
     * Retrieves the status of all jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of jobs.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "listJobs",
            summary = "Retrieves jobs",
            description = "Fetches a paginated list of all jobs regardless of state. Results can be paginated using query parameters.",
            tags        = {"Job Queue"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of jobs.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have necessary permissions to view jobs."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while retrieving jobs.")
            }
    )
    public ResponseEntityView<JobPaginatedResult> listJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(
                    description = "Number of records per page."
            )
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

    /**
     * Retrieves the status of active jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of active jobs.
     */
    @GET
    @Path("/active")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getActiveJobs",
            summary = "Retrieves active jobs",
            description = "Fetches a paginated list of active jobs. Results can be paginated using query parameters.",
            tags        = {"Job Queue"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of active jobs.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have necessary permissions to view jobs."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while retrieving jobs.")
            }
    )
    public ResponseEntityView<JobPaginatedResult> activeJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(
                    description = "Number of records per page."
            )
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

    /**
     * Retrieves the status of completed jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of completed jobs.
     */
    @GET
    @Path("/completed")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getCompletedJobs",
            summary = "Retrieves completed jobs",
            description = "Fetches a paginated list of completed jobs. Results can be paginated using query parameters.",
            tags        = {"Job Queue"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of completed jobs.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have necessary permissions to view jobs."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while retrieving jobs.")
            }
    )
    public ResponseEntityView<JobPaginatedResult> completedJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(
                    description = "Number of records per page."
            )
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

    /**
     * Retrieves the status of successful jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of successful jobs.
     */
    @GET
    @Path("/successful")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getSuccessfulJobs",
            summary = "Retrieves successful jobs",
            description = "Fetches a paginated list of successful jobs. Results can be paginated using query parameters.",
            tags        = {"Job Queue"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of successful jobs.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have necessary permissions to view jobs."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while retrieving jobs.")
            }
    )
    public ResponseEntityView<JobPaginatedResult> successfulJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("page")
            @DefaultValue("1")
            @Parameter(description = "Page number to retrieve (1-based).")
            int page,
            @QueryParam("pageSize")
            @DefaultValue("20")
            @Parameter(description = "Number of records per page.")
            int pageSize) {
        new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        final JobPaginatedResult result = helper.getSuccessfulJobs(page, pageSize);
        return new ResponseEntityView<>(result);
    }

    /**
     * Retrieves the status of canceled jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of canceled jobs.
     */
    @GET
    @Path("/canceled")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getCanceledJobs",
            summary     = "Retrieves canceled jobs",
            description = "Fetches a paginated list of canceled jobs. Results can be paginated using query parameters.",
            tags        = {"Job Queue"},
            responses   = {
                    @ApiResponse(
                            responseCode = "200",
                            description  = "Successfully retrieved the paginated list of canceled jobs.",
                            content      = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema    = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User lacks permission to view jobs."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while retrieving jobs.")
            }
    )
    public ResponseEntityView<JobPaginatedResult> canceledJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(
                    description = "Number of records per page."
            )
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

    /**
     * Retrieves the status of failed jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of failed jobs.
     */
    @GET
    @Path("/failed")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getFailedJobs",
            summary     = "Retrieves failed jobs",
            description = "Fetches a paginated list of failed jobs. Results can be paginated using query parameters.",
            tags        = {"Job Queue"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of failed jobs.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have necessary permissions to view jobs."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while retrieving jobs.")
            }
    )
    public ResponseEntityView<JobPaginatedResult> failedJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(
                    description = "Number of records per page."
            )
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

    /**
     * Retrieves the status of abandoned jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of abandoned jobs.
     */
    @GET
    @Path("/abandoned")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getAbandonedJobs",
            summary = "Retrieves abandoned jobs",
            description = "Fetches a paginated list of abandoned jobs. Results can be paginated using query parameters.",
            tags        = {"Job Queue"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of abandoned jobs.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have necessary permissions to view jobs."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while retrieving jobs.")
            }
    )
    public ResponseEntityView<JobPaginatedResult> abandonedJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(
                    description = "Number of records per page."
            )
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

    /**
     * Monitors the progress of a specific job identified by its jobId.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param jobId The ID of the job whose progress is to be monitored.
     * @return An EventOutput for real-time job progress updates.
     */
    @GET
    @Path("/{jobId}/monitor")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @SuppressWarnings("java:S1854") // jobWatcher assignment is needed for cleanup in catch blocks
    @Operation(
            operationId = "monitorJob",
            summary     = "Monitor a job in real time",
            description = "Establishes a Server-Sent Events (SSE) connection to monitor the progress of a specific  job in real-time. This endpoint will continuously send updates as the job progresses, including status changes and completion information.",
            tags        = {"Job Queue"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Server-Sent Events stream established successfully. Events will be sent as the job progresses.",
                            content = @Content(
                                    mediaType = SseFeature.SERVER_SENT_EVENTS,
                                    schema = @Schema(implementation = EventOutput.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have permissions to monitor the specified job."),
                    @ApiResponse(responseCode = "404", description = "Not Found: Job with the specified ID could not be found."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while establishing the monitoring connection.")
            }
    )
    public EventOutput monitorJob(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("jobId")
            @Parameter(
                    name = "jobId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "The unique identifier (UUID) of the job whose status is to be retrieved.",
                    schema = @Schema(type = "string", format = "uuid", example = "e6d9bae8-657b-4e2f-8524-c0222db66355")
            )
            String jobId) {

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