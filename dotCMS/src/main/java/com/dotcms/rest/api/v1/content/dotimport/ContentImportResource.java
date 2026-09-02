package com.dotcms.rest.api.v1.content.dotimport;

import static com.dotcms.rest.api.v1.content.dotimport.ContentImportHelper.CMD_PREVIEW;
import static com.dotcms.rest.api.v1.content.dotimport.ContentImportHelper.CMD_PUBLISH;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.job.JobView;
import com.dotcms.jobs.business.job.JobViewPaginatedResult;
import com.dotcms.rest.ResponseEntityJobPaginatedResultView;
import com.dotcms.rest.ResponseEntityJobStatusView;
import com.dotcms.rest.ResponseEntityJobView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.job.SSEMonitorUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
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

/**
 * REST resource for handling content import operations, including creating and enqueuing content import jobs.
 * This class provides endpoints for importing content from CSV files and processing them based on the provided parameters.
 */
@SwaggerCompliant(value = "Content management and workflow APIs", batch = 2)
@Path("/v1/content/_import")
@Tag(name = "Content")
public class ContentImportResource {

    private final WebResource webResource;
    private final ContentImportHelper importHelper;
    private final SSEMonitorUtil sseMonitorUtil;

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
                    description = ContentImportDocs.FORM_FIELD_DOC,
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA,
                            schema = @Schema(implementation = ContentImportParamsSchema.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Content import job successfully created and enqueued.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseEntityJobStatusView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request: Invalid parameters or malformed request (e.g., missing file, invalid JSON in 'form', file not CSV)."),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have necessary permissions for content import or workflow action."),
                    @ApiResponse(responseCode = "404", description = "Not Found: Specified Content Type or Language could not be found."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred during job creation or processing.")
            }
    )
    public Response importContent(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam final ContentImportParams params)
            throws DotDataException, IOException {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        return importHelper.handleJobCreation(
                CMD_PUBLISH, params, initDataObject, request
        );
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
            description = "Creates and enqueues a content import job in preview mode. This validates the CSV data against the specified Content Type, language, and workflow action without actually importing content.",
            tags = {"Content Import"},
            requestBody = @RequestBody(
                    required = true,
                    description = ContentImportDocs.FORM_FIELD_DOC,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA,
                            schema = @Schema(implementation = ContentImportParamsSchema.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Content import validation job successfully created and enqueued.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
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
                    @ApiResponse(responseCode = "400", description = "Bad Request: Invalid parameters or malformed request (e.g., missing file, invalid JSON in 'form', file not CSV)."),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have necessary permissions for validation or workflow action."),
                    @ApiResponse(responseCode = "404", description = "Not Found: Specified Content Type or Language could not be found."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred during job creation or processing.")
            }
    )
    public Response validateContentImport(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam final ContentImportParams params)
            throws DotDataException, IOException {

        // Initialize the WebResource and set required user information
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        return importHelper.handleJobCreation(
                CMD_PREVIEW, params, initDataObject, request
        );
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
            description = "Fetches the detailed current status of a specific content import job identified by its ID.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved job status. The entity contains detailed information about the job.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseEntityJobView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have permissions to view the specified job."),
                    @ApiResponse(responseCode = "404", description = "Not Found: Job with the specified ID could not be found."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while retrieving the job status.")
            }
    )
    public ResponseEntityView<JobView> getJobStatus(
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
            final String jobId
    ) throws DotDataException {

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
            description = "Requests cancellation of a specific content import job identified by its ID. Note that cancellation is asynchronous and may not be immediate.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Cancellation request successfully sent to the job.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseEntityStringView.class),
                                    examples = @ExampleObject(value = "{\n" +
                                            "  \"entity\": \"Cancellation request successfully sent to job e6d9bae8-657b-4e2f-8524-c0222db66355\",\n" +
                                            "  \"errors\": [],\n" +
                                            "  \"i18nMessagesMap\": {}," +
                                            "  \"messages\": [],\n" +
                                            "  \"pagination\": null,\n" +
                                            "  \"permissions\": []\n" +
                                            "}")
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing user authentication."),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User does not have permissions to cancel the specified job."),
                    @ApiResponse(responseCode = "404", description = "Not Found: Job with the specified ID could not be found or is already completed/cancelled."),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: An unexpected error occurred while attempting to cancel the job.")
            }
    )
    public ResponseEntityView<String> cancelJob(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("jobId")
            @Parameter(
                name = "jobId",
                in = ParameterIn.PATH,
                required = true,
                description = "The unique identifier (UUID) of the job to be cancelled.",
                schema = @Schema(type = "string", format = "uuid", example = "e6d9bae8-657b-4e2f-8524-c0222db66355")
            )
            final String jobId
    ) throws DotDataException {
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
            summary = "Retrieves content import jobs",
            description = "Fetches a paginated list of all content import jobs regardless of state. Results can be paginated using query parameters.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of content import jobs.",
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
    public ResponseEntityView<JobViewPaginatedResult> listJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") final int page,
            @Parameter(
                    description = "Number of records per page."
            )
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
            summary = "Retrieves active content import jobs",
            description = "Fetches a paginated list of active content import jobs (jobs with state NEW, PROCESSING, or WAITING). Results can be paginated using query parameters.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of active content import jobs.",
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
    public ResponseEntityView<JobViewPaginatedResult> activeJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") final int page,
            @Parameter(
                    description = "Number of records per page."
            )
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
            summary = "Retrieves completed content import jobs",
            description = "Fetches a paginated list of completed content import jobs (jobs with state COMPLETED). Results can be paginated using query parameters.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of completed content import jobs.",
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
    public ResponseEntityView<JobViewPaginatedResult> completedJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") final int page,
            @Parameter(
                    description = "Number of records per page."
            )
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
            summary = "Retrieves canceled content import jobs",
            description = "Fetches a paginated list of canceled content import jobs (jobs with state CANCELED). Results can be paginated using query parameters.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of canceled content import jobs.",
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
    public ResponseEntityView<JobViewPaginatedResult> canceledJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") final int page,
            @Parameter(
                    description = "Number of records per page."
            )
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
            summary = "Retrieves failed content import jobs",
            description = "Fetches a paginated list of failed content import jobs (jobs with state FAILED). Results can be paginated using query parameters.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of failed content import jobs.",
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
    public ResponseEntityView<JobViewPaginatedResult> failedJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") final int page,
            @Parameter(
                    description = "Number of records per page."
            )
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
            summary = "Retrieves abandoned content import jobs",
            description = "Fetches a paginated list of abandoned content import jobs (jobs with state ABANDONED). Results can be paginated using query parameters.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of abandoned content import jobs.",
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
    public ResponseEntityView<JobViewPaginatedResult> abandonedJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") final int page,

            @Parameter(
                    description = "Number of records per page."
            )
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
            summary = "Retrieves successful content import jobs",
            description = "Fetches a paginated list of successful content import jobs (jobs with state COMPLETED and successful result). Results can be paginated using query parameters.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the paginated list of successful content import jobs.",
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
    public ResponseEntityView<JobViewPaginatedResult> successfulJobs(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(
                    description = "Page number to retrieve (1-based indexing)."
            )
            @QueryParam("page") @DefaultValue("1") final int page,
            @Parameter(
                    description = "Number of records per page."
            )
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
            summary = "Monitor a content import job in real-time",
            description = "Establishes a Server-Sent Events (SSE) connection to monitor the progress of a specific content import job in real-time. This endpoint will continuously send updates as the job progresses, including status changes and completion information.",
            tags = {"Content Import"},
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
            final String jobId
    ) {

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