package com.dotcms.rest.api.v1.asset.bulkdelete;

import com.dotcms.rest.ResponseEntityFolderBulkDeleteSubmitView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.exception.DotDataException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Deletes several folders from one submission, as a background run (#37063).
 * <p>
 * Content Drive lets an author select several folders but the only folder delete that exists is
 * the single-folder context menu item ({@code POST /v1/assets/folders/_delete}, #35161), unchanged
 * here (FR-006). This endpoint answers {@code 202} with a job handle before any folder is deleted;
 * follow it with {@code GET /api/v1/jobs/{jobId}/status}, cancel it with
 * {@code POST /api/v1/jobs/{jobId}/cancel}. One folder failing does not abort the rest — every
 * accepted path produces exactly one outcome record (FR-014).
 *
 * @author dotCMS
 */
@Path("/v1/assets")
@Tag(name = "Web Assets")
@ApplicationScoped
public class FolderBulkDeleteResource {

    private final WebResource webResource;
    private final FolderBulkDeleteHelper helper;

    /** Required by CDI for proxying, never called by this code — see {@link FolderBulkDeleteHelper}. */
    public FolderBulkDeleteResource() {
        this.webResource = new WebResource();
        this.helper = null;
    }

    @Inject
    public FolderBulkDeleteResource(final FolderBulkDeleteHelper helper) {
        this.webResource = new WebResource();
        this.helper = helper;
    }

    /**
     * Accepts a selection of folder paths and answers immediately with a handle.
     */
    @POST
    @Path("/folders/_bulkdelete")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "bulkDeleteFolders",
            summary = "Delete several folders from one submission, asynchronously",
            description = "Accepts a list of folder paths and answers `202` with a job handle "
                    + "**before any folder is deleted**. The run is asynchronous, queued to the "
                    + "`folderBulkDelete` queue, and each folder's delete is recursive and "
                    + "permanent, exactly as the single-folder delete already is. Follow the run "
                    + "with `GET /api/v1/jobs/{jobId}/status`, cancel it with "
                    + "`POST /api/v1/jobs/{jobId}/cancel`, and watch it with "
                    + "`GET /api/v1/jobs/{jobId}/monitor`. The submitting author is notified on "
                    + "any terminal state.\n\n"
                    + "One folder failing does not abort the run: every remaining folder is still "
                    + "attempted, and each failure is reported by path with a machine-readable "
                    + "reason.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Run queued",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityFolderBulkDeleteSubmitView.class))),
                    @ApiResponse(responseCode = "400", description =
                            "No paths submitted, or more paths than the configured maximum"),
                    @ApiResponse(responseCode = "403", description =
                            "The caller is not entitled to use this operation"),
                    @ApiResponse(responseCode = "409", description =
                            "A submitted path overlaps an in-flight run's paths")
            })
    public Response bulkDelete(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               final FolderBulkDeleteForm form)
            throws DotDataException {

        final var initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final FolderBulkDeleteSubmitResponse submitted = helper.submit(form, initData.getUser());

        return Response.status(Response.Status.ACCEPTED)
                .entity(new ResponseEntityFolderBulkDeleteSubmitView(submitted))
                .build();
    }
}
