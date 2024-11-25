package com.dotcms.rest.api.v1.content._import;

import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

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
@Path("/v1/content")
public class ContentImportResource {

    private final WebResource webResource;
    private final ContentImportHelper importHelper;
    private static final String IMPORT_QUEUE_NAME = "importContentlets";
    
    // Constants for commands
    private static final String CMD_PUBLISH = com.dotmarketing.util.Constants.PUBLISH;
    
    /**
     * Constructor for ContentImportResource.
     *
     * @param importHelper The helper class used to manage content import jobs
     */
    @Inject
    public ContentImportResource(final ContentImportHelper importHelper) {
        this(new WebResource(), importHelper);
    }

    /**
     * Constructor for ContentImportResource with WebResource and ContentImportHelper injected.
     *
     * @param webResource The web resource for handling HTTP requests and responses
     * @param importHelper The helper class used to manage content import jobs
     */
    public ContentImportResource(final WebResource webResource, final ContentImportHelper importHelper) {
        this.webResource = webResource;
        this.importHelper = importHelper;
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
    @Path("/_import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "importContent",
            summary = "Imports content from a CSV file",
            description = "Creates and enqueues a new content import job based on the provided parameters. The job processes a CSV file and updates content based on the specified content type, language, and workflow action.",
            tags = {"Content Import"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Content import job created successfully",
                            content = @Content(mediaType = "application/json",
                                    examples = @ExampleObject(value = "{\n" +
                                            "  \"entity\": \"3930f815-7aa4-4649-94c2-3f37fd21136d\",\n" +
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
            // Create the import job
            final String jobId = importHelper.createJob(CMD_PUBLISH, IMPORT_QUEUE_NAME, params, initDataObject.getUser(), request);
            return Response.ok(new ResponseEntityView<>(jobId)).build();
        } catch (JobValidationException e) {
            // Handle validation exception and return appropriate error message
            return ExceptionMapperUtil.createResponse(null, e.getMessage());
        }
    }
}