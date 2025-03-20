package com.dotcms.rest.api.v1.content.dotimport;

import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.job.JobView;
import com.dotcms.jobs.business.job.JobViewPaginatedResult;
import com.dotcms.repackage.javax.validation.ValidationException;
import com.dotcms.rest.*;
import com.dotcms.rest.api.v1.job.JobResponseUtil;
import com.dotcms.rest.ResponseEntityJobStatusView;
import com.dotcms.rest.api.v1.job.SSEMonitorUtil;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * REST resource for handling content import operations, including creating and enqueuing content import jobs.
 * This class provides endpoints for importing content from CSV files and processing them based on the provided parameters.
 */
@Path("/v1/content/_import")
public class ContentImportResource {

    private final WebResource webResource;
    private final ContentImportHelper importHelper;
    private final SSEMonitorUtil sseMonitorUtil;
    private static final String IMPORT_QUEUE_NAME = "importContentlets";
    
    // Constants for commands
    private static final String CMD_PUBLISH = Constants.PUBLISH;
    private static final String CMD_PREVIEW = Constants.PREVIEW;


    /**
     * Constructor for ContentImportResource.
     *
     * @param importHelper The helper class used to manage content import jobs
     */
    @Inject
    public ContentImportResource(final ContentImportHelper importHelper, final SSEMonitorUtil sseMonitorUtil) {
        this(new WebResource(), importHelper, sseMonitorUtil);
    }

    /**
     * Constructor for ContentImportResource with WebResource and ContentImportHelper injected.
     *
     * @param webResource The web resource for handling HTTP requests and responses
     * @param importHelper The helper class used to manage content import jobs
     */
    public ContentImportResource(final WebResource webResource, final ContentImportHelper importHelper, final SSEMonitorUtil sseMonitorUtil) {
        this.webResource = webResource;
        this.importHelper = importHelper;
        this.sseMonitorUtil = sseMonitorUtil;
    }

    /**
     * Creates and enqueues a new content import job, processing a CSV file with specified parameters.
     *
     * @param request The HTTP servlet request containing user and context information
     * @param response The HTTP servlet response that will contain the response to the client
     * @param params The import parameters, including:
     *               - file: The CSV file to import
     *               - contentType: The content type variable or ID (required)
     *               - language: The language code (e.g., "en-US") or ID
     *               - workflowActionId: The workflow action ID to apply (required)
     *               - fields: List of fields to use as keys for updates
     *
     * @return A Response containing the job ID if the import job was successfully created, or an error response if validation fails
     * @throws DotDataException If there is an issue with DotData during the import process
     * @throws JsonProcessingException If there is an issue processing the JSON response
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "importContent",
            summary = "Imports content from a CSV file",
            description = "Creates and enqueues a new content import job. Requires a CSV file and a JSON string representing import parameters.",
            tags = {"Content Import"},
            requestBody = @RequestBody(
                    required = true,
                    description = "Import parameters including the file to import and a JSON string for import settings.",
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(
                                    type = "object",
                                    requiredProperties = {"file", "form"},
                                    implementation = ContentImportParamsSchema.class
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Content import job created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobStatusView.class),
                                    examples = @ExampleObject(value = "{\n" +
                                            "  \"entity\": {\n" +
                                            "    \"jobId\": \"e6d9bae8-657b-4e2f-8524-c0222db66355\",\n" +
                                            "    \"statusUrl\": \"http://localhost:8080/api/v1/content/_import/e6d9bae8-657b-4e2f-8524-c0222db66355\"\n" +
                                            "  },\n" +
                                            "  \"errors\": [],\n" +
                                            "  \"i18nMessagesMap\": {},\n" +
                                            "  \"messages\": [],\n" +
                                            "  \"pagination\": null,\n" +
                                            "  \"permissions\": []\n" +
                                            "}")
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request due to validation errors"),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "Content type or language not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response importContent(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam final ContentImportParams params)
            throws DotDataException, JsonProcessingException {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, ()->String.format(" user %s is importing content: %s", initDataObject.getUser().getUserId(), params));

        try {
            // Create the content import job
            final String jobId = importHelper.createJob(CMD_PUBLISH, IMPORT_QUEUE_NAME, params, initDataObject.getUser(), request);

            final var jobStatusResponse = JobResponseUtil.buildJobStatusResponse(jobId, request);
            return Response.ok(new ResponseEntityJobStatusView(jobStatusResponse)).build();
        } catch (JobValidationException | ValidationException e) {
            // Handle validation exception and return appropriate error message
            return ExceptionMapperUtil.createResponse(null, e.getMessage());
        }
    }

    /**
     * Validates the content import parameters and creates a preview job for importing content from a CSV file.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param params The import parameters, including:
     *               - file: The CSV file to import
     *               - contentType: The content type variable or ID (required)
     *               - language: The language code (e.g., "en-US") or ID
     *               - language: The language code (e.g., "en-US") or ID
     *               - workflowActionId: The workflow action ID to apply (required)
     *               - fields: List of fields to use as keys for updates
     *
     * @return A Response containing the job ID if the import job was successfully created, or an error response if validation fails.
     * @throws DotDataException If there is an issue with DotData during the import process.
     * @throws JsonProcessingException If there is an issue processing the JSON response.
     */
    @POST
    @Path("/_validate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "validateContentImport",
            summary = "Validates content import from a CSV file",
            description = "Creates and enqueues a new content import job in preview mode based on the provided parameters. The job processes a CSV file and validates the content based on the specified content type, language, and workflow action.",
            tags = {"Content Import"},
            requestBody = @RequestBody(
                    required = true,
                    description = "Import parameters including the file to import and a JSON string for import settings.",
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(
                                    type = "object",
                                    requiredProperties = {"file", "form"},
                                    implementation = ContentImportParamsSchema.class
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Content import job in preview mode created successfully",
                            content = @Content(
                                    mediaType = "application/json",
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
                    @ApiResponse(responseCode = "400", description = "Bad request due to validation errors"),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "Content type or language not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response validateContentImport(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam final ContentImportParams params)
            throws DotDataException, JsonProcessingException {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, ()->String.format(" user %s is importing content in preview mode: %s", initDataObject.getUser().getUserId(), params));

        try {
            // Create the content import job in preview mode
            final String jobId = importHelper.createJob(CMD_PREVIEW, IMPORT_QUEUE_NAME, params, initDataObject.getUser(), request);

            final var jobStatusResponse = JobResponseUtil.buildJobStatusResponse(jobId, request);
            return Response.ok(new ResponseEntityJobStatusView(jobStatusResponse)).build();
        } catch (JobValidationException | ValidationException e) {
            // Handle validation exception and return appropriate error message
            return ExceptionMapperUtil.createResponse(null, e.getMessage());
        }
    }

    /**
     * Retrieves the status of a content import job based on the provided job ID.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param jobId The ID of the job whose status is to be retrieved.
     * @return A ResponseEntityView containing the job status.
     * @throws DotDataException If there is an issue with DotData during the retrieval process.
     */
    @GET
    @Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getJobStatus",
            summary = "Retrieves the status of a content import job",
            description = "Fetches the current status of a content import job based on the provided job ID.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved job status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "Job not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<JobView> getJobStatus(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("jobId") @Parameter(
                required = true,
                description = "The ID of the job whose status is to be retrieved",
                schema = @Schema(type = "string")
            ) final String jobId)
            throws DotDataException {

        // Initialize the WebResource and set required user information
        final var initDataObject =  new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, ()->String.format(" user %s is retrieving status of job: %s", initDataObject.getUser().getUserId(), jobId));

        Job job = importHelper.getJob(jobId);
        return new ResponseEntityView<>(importHelper.view(job));
    }

    /**
     * Cancels a content import job based on the provided job ID.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param jobId The ID of the job to be canceled.
     * @return A ResponseEntityView containing a message indicating the cancellation status.
     * @throws DotDataException If there is an issue with DotData during the cancellation process.
     */
    @POST
    @Path("/{jobId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "cancelContentImportJob",
            summary = "Cancel a content import job",
            description = "Cancel a content import job based on the provided job ID.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully cancelled content import job",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "Job not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<String> cancelJob(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("jobId") @Parameter(
                    required = true,
                    description = "The ID of the job whose status is to be retrieved",
                    schema = @Schema(type = "string")
            ) final String jobId) throws DotDataException {
        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, () -> String.format("User %s is cancelling content import jobs with id '%s'",
                initDataObject.getUser().getUserId(),
                jobId));

        importHelper.cancelJob(jobId);
        return new ResponseEntityView<>("Cancellation request successfully sent to job " + jobId);
    }

    /**
     * Retrieves the status of all content import jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of content import jobs.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getContentImportJobs",
            summary = "Retrieves the status of a content import jobs",
            description = "Fetches the current status of all content import jobs.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved content import jobs status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<JobViewPaginatedResult> listJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") @DefaultValue("20") final int pageSize) {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, () -> String.format("User %s is listing content import jobs",
                initDataObject.getUser().getUserId()));

        final JobPaginatedResult result = importHelper.getJobs(page, pageSize);

        return new ResponseEntityView<>(importHelper.view(result));
    }

    /**
     * Retrieves the status of active content import jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of active content import jobs.
     */
    @GET
    @Path("/active")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getActiveContentImportJobs",
            summary = "Retrieves the status of active content import jobs",
            description = "Fetches the current status of active content import jobs.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved active content import jobs status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<JobViewPaginatedResult> activeJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") @DefaultValue("20") final int pageSize) {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, () -> String.format("User %s is listing active content import jobs",
                initDataObject.getUser().getUserId()));

        final JobPaginatedResult result = importHelper.getActiveJobs(page, pageSize);
        return new ResponseEntityView<>(importHelper.view(result));
    }

    /**
     * Retrieves the status of completed content import jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of completed content import jobs.
     */
    @GET
    @Path("/completed")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getCompletedContentImportJobs",
            summary = "Retrieves the status of completed content import jobs",
            description = "Fetches the current status of completed content import jobs.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved completed content import jobs status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<JobViewPaginatedResult> completedJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") @DefaultValue("20") final int pageSize) {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, () -> String.format("User %s is listing completed content import jobs",
                initDataObject.getUser().getUserId()));

        final JobPaginatedResult result = importHelper.getCompletedJobs(page, pageSize);
        return new ResponseEntityView<>(importHelper.view(result));
    }

    /**
     * Retrieves the status of canceled content import jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of canceled content import jobs.
     */
    @GET
    @Path("/canceled")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getCanceledContentImportJobs",
            summary = "Retrieves the status of canceled content import jobs",
            description = "Fetches the current status of canceled content import jobs.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved canceled content import jobs status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<JobViewPaginatedResult> canceledJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") @DefaultValue("20") final int pageSize) {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, () -> String.format("User %s is listing canceled content import jobs",
                initDataObject.getUser().getUserId()));

        final JobPaginatedResult result = importHelper.getCanceledJobs(page, pageSize);
        return new ResponseEntityView<>(importHelper.view(result));
    }

    /**
     * Retrieves the status of failed content import jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of failed content import jobs.
     */
    @GET
    @Path("/failed")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getFailedContentImportJobs",
            summary = "Retrieves the status of failed content import jobs",
            description = "Fetches the current status of failed content import jobs.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved failed content import jobs status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<JobViewPaginatedResult> failedJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") @DefaultValue("20") final int pageSize) {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, () -> String.format("User %s is listing failed content import jobs",
                initDataObject.getUser().getUserId()));

        final JobPaginatedResult result = importHelper.getFailedJobs(page, pageSize);
        return new ResponseEntityView<>(importHelper.view(result));
    }

    /**
     * Retrieves the status of abandoned content import jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of abandoned content import jobs.
     */
    @GET
    @Path("/abandoned")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getAbandonedContentImportJobs",
            summary = "Retrieves the status of abandoned content import jobs",
            description = "Fetches the current status of abandoned content import jobs.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved abandoned content import jobs status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<JobViewPaginatedResult> abandonedJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") @DefaultValue("20") final int pageSize) {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, () -> String.format("User %s is listing abandoned content import jobs",
                initDataObject.getUser().getUserId()));

        final JobPaginatedResult result = importHelper.getAbandonedJobs(page, pageSize);
        return new ResponseEntityView<>(importHelper.view(result));
    }

    /**
     * Retrieves the status of successful content import jobs.
     *
     * @param request The HTTP servlet request containing user and context information.
     * @param response The HTTP servlet response that will contain the response to the client.
     * @param page The page number for pagination (default is 1).
     * @param pageSize The number of jobs per page (default is 20).
     * @return A ResponseEntityView containing the paginated result of successful content import jobs.
     */
    @GET
    @Path("/successful")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getSuccessfulContentImportJobs",
            summary = "Retrieves the status of successful content import jobs",
            description = "Fetches the current status of successful content import jobs.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved successful content import jobs status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntityView<JobViewPaginatedResult> successfulJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") @DefaultValue("20") final int pageSize) {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, () -> String.format("User %s is listing successful content import jobs",
                initDataObject.getUser().getUserId()));

        final JobPaginatedResult result = importHelper.getSuccessfulJobs(page, pageSize);
        return new ResponseEntityView<>(importHelper.view(result));
    }

    /**
     * Monitors the progress of a specific content import job identified by its jobId.
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
            operationId = "monitorContentImportJobs",
            summary = "Monitor a specific content import job progress",
            description = "Allows clients to monitor the progress of a specific content import job identified by its jobId. " +
                    "The response uses Server-Sent Events (SSE) to provide real-time updates.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Real-time job progress updates",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityJobPaginatedResultView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid user authentication"),
                    @ApiResponse(responseCode = "403", description = "Forbidden due to insufficient permissions"),
                    @ApiResponse(responseCode = "404", description = "Job not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public EventOutput monitorJob(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("jobId") @Parameter(
                    required = true,
                    description = "The ID of the job whose status is to be retrieved",
                    schema = @Schema(type = "string")
            ) final String jobId) {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, () -> String.format("User %s is monitoring content import job %s",
                initDataObject.getUser().getUserId(), jobId));

        return sseMonitorUtil.monitorJob(jobId);
    }
}