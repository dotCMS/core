package com.dotcms.rest.api.v1.contentImport;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.VisibleForTesting;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;


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
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<String> importContent(
            @Context final HttpServletRequest request,
            @BeanParam final ContentImportParams params)
            throws DotDataException, JsonProcessingException {
        final var initDataObject = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();

        final String jobId = importHelper.createJob(CMD_PUBLISH, IMPORT_QUEUE_NAME, params, initDataObject.getUser(), request);
        return new ResponseEntityView<>(jobId);
    }
} 