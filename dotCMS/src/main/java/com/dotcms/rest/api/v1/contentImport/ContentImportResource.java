package com.dotcms.rest.api.v1.contentImport;

import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.VisibleForTesting;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path("/v1/content")
public class ContentImportResource {

    private final WebResource webResource;
    private final ContentImportHelper importHelper;
    private final String IMPORT_QUEUE_NAME = "importContentlets";

    //TODO move to a common place
    private static final String CMD_PREVIEW = "preview";
    private static final String CMD_PUBLISH = "publish";

    @Inject
    public ContentImportResource(final ContentImportHelper importHelper) {
        this(new WebResource(), importHelper);
    }

    public ContentImportResource(final WebResource webResource, final ContentImportHelper importHelper) {
        this.webResource = webResource;
        this.importHelper = importHelper;
    }

    /**
     * Creates and enqueues a new content import job
     * 
     * @param request HTTP request
     * @param params The import parameters including:
     *              - file: The CSV file to import
     *              - params: JSON object containing:
     *                  - contentType: The content type variable or ID (required)
     *                  - language: The language code (e.g., "en-US") or ID
     *                  - workflowActionId: The workflow action ID to apply (required)
     *                  - fields: List of fields to use as key for updates
     * @return Response containing the job ID
     */
    @POST
    @Path("/_import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importContent(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam final ContentImportParams params)
            throws DotDataException, JsonProcessingException {
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        try{
            final String jobId = importHelper.createJob(CMD_PUBLISH, IMPORT_QUEUE_NAME, params, initDataObject.getUser(), request);
            return Response.ok(new ResponseEntityView<>(jobId)).build();
        }catch (JobValidationException e) {
            return ExceptionMapperUtil.createResponse(null, e.getMessage());
        }
    }
} 